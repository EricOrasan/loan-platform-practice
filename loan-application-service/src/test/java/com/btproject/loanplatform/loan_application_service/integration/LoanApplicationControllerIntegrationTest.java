package com.btproject.loanplatform.loan_application_service.integration;

import com.btproject.loanplatform.loan_application_service.TestcontainersConfiguration;
import com.btproject.loanplatform.loan_application_service.application.port.out.LoanApplicationRepository;
import com.btproject.loanplatform.loan_application_service.domain.LoanApplication;
import com.btproject.loanplatform.loan_application_service.domain.LoanApplicationStatus;
import com.btproject.loanplatform.loan_application_service.infrastructure.messaging.LoanApplicationCreatedEvent;
import com.btproject.loanplatform.loan_application_service.infrastructure.persistence.LoanApplicationJpaEntity;
import com.btproject.loanplatform.loan_application_service.infrastructure.persistence.SpringDataLoanApplicationRepository;
import com.btproject.loanplatform.loan_application_service.infrastructure.web.dto.CreateLoanApplicationRequest;
import com.btproject.loanplatform.loan_application_service.infrastructure.web.dto.LoanApplicationResponse;
import com.btproject.loanplatform.loan_application_service.infrastructure.web.dto.UpdateLoanApplicationRequest;
import com.btproject.loanplatform.loan_application_service.infrastructure.web.error.ApiErrorResponse;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.kafka.KafkaContainer;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestRestTemplate
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoanApplicationControllerIntegrationTest {

    private static final String CIF = "12345678";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private LoanApplicationRepository domainRepository;

    @Autowired
    private SpringDataLoanApplicationRepository jpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaContainer kafkaContainer;

    @Value("${app.kafka.topics.loan-application-created}")
    private String applicationCreatedTopic;

    @BeforeEach
    void cleanDatabase() {
        jpaRepository.deleteAll();
    }

    @Test
    void shouldCreatePersistAndPublishLoanApplication() throws Exception {
        try (KafkaConsumer<String, String> consumer = createKafkaConsumer()) {
            consumer.subscribe(List.of(applicationCreatedTopic));

            ResponseEntity<LoanApplicationResponse> response = userClient().postForEntity(
                    "/applications",
                    validCreateRequest(),
                    LoanApplicationResponse.class
            );

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            LoanApplicationResponse body = response.getBody();
            assertNotNull(body);
            assertNotNull(body.id());
            assertTrue(body.applicationNumber().startsWith("LA-"));
            assertEquals(CIF, body.cif());
            assertEquals(LoanApplicationStatus.DRAFT, body.status());

            LoanApplicationJpaEntity persisted = jpaRepository.findById(body.id()).orElseThrow();
            assertEquals(CIF, persisted.getCif());
            assertEquals(0, persisted.getRequestedAmount().compareTo(BigDecimal.valueOf(42_000)));
            assertNotNull(persisted.getCreatedAt());
            assertNotNull(persisted.getUpdatedAt());

            ConsumerRecord<String, String> record = awaitRecord(consumer, body.id());
            LoanApplicationCreatedEvent event = objectMapper.readValue(
                    record.value(),
                    LoanApplicationCreatedEvent.class
            );

            assertEquals(body.id().toString(), record.key());
            assertNotNull(event.eventId());
            assertEquals("LOAN_APPLICATION_CREATED", event.eventType());
            assertEquals(body.id(), event.applicationId());
            assertEquals(CIF, event.cif());
            assertEquals(0, event.requestedAmount().compareTo(BigDecimal.valueOf(42_000)));
            assertEquals(60, event.requestedPeriodMonths());
            assertNotNull(event.createdAt());
        }
    }

    @Test
    void shouldGetPersistedLoanApplication() {
        LoanApplication persisted = persistDraftApplication();

        ResponseEntity<LoanApplicationResponse> response = userClient().getForEntity(
                "/applications/{id}",
                LoanApplicationResponse.class,
                persisted.getId()
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(persisted.getId(), response.getBody().id());
        assertEquals(LoanApplicationStatus.DRAFT, response.getBody().status());
    }

    @Test
    void shouldUpdateDraftLoanApplication() {
        LoanApplication persisted = persistDraftApplication();
        UpdateLoanApplicationRequest request = new UpdateLoanApplicationRequest(
                BigDecimal.valueOf(50_000),
                72,
                "Updated integration test purpose"
        );

        ResponseEntity<LoanApplicationResponse> response = userClient().exchange(
                "/applications/{id}",
                HttpMethod.PUT,
                new HttpEntity<>(request),
                LoanApplicationResponse.class,
                persisted.getId()
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().requestedAmount().compareTo(BigDecimal.valueOf(50_000)));
        assertEquals(72, response.getBody().requestedPeriodMonths());

        LoanApplicationJpaEntity updated = jpaRepository.findById(persisted.getId()).orElseThrow();
        assertEquals(0, updated.getRequestedAmount().compareTo(BigDecimal.valueOf(50_000)));
        assertEquals("Updated integration test purpose", updated.getPurpose());
        assertTrue(updated.getUpdatedAt().compareTo(updated.getCreatedAt()) >= 0);
    }

    @Test
    void shouldMarkLoanApplicationAsDeletedForAdmin() {
        LoanApplication persisted = persistDraftApplication();

        ResponseEntity<Void> response = adminClient().exchange(
                "/applications/{id}",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class,
                persisted.getId()
        );

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        LoanApplicationJpaEntity deleted = jpaRepository.findById(persisted.getId()).orElseThrow();
        assertEquals(LoanApplicationStatus.DELETED, deleted.getStatus());
    }

    @Test
    void shouldReturnValidationErrorForInvalidRequest() {
        CreateLoanApplicationRequest request = new CreateLoanApplicationRequest(
                "12",
                BigDecimal.ZERO,
                3,
                ""
        );

        ResponseEntity<ApiErrorResponse> response = userClient().postForEntity(
                "/applications",
                request,
                ApiErrorResponse.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertErrorResponse(response.getBody(), "VALIDATION_ERROR");
        assertEquals(0, jpaRepository.count());
    }

    @Test
    void shouldReturnNotFoundForUnknownApplication() {
        ResponseEntity<ApiErrorResponse> response = userClient().getForEntity(
                "/applications/{id}",
                ApiErrorResponse.class,
                UUID.randomUUID()
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertErrorResponse(response.getBody(), "APPLICATION_NOT_FOUND");
    }

    @Test
    void shouldReturnConflictWhenUpdatingApprovedApplication() {
        LoanApplication persisted = persistDraftApplication();
        setStatus(persisted.getId(), LoanApplicationStatus.APPROVED);

        ResponseEntity<ApiErrorResponse> response = userClient().exchange(
                "/applications/{id}",
                HttpMethod.PUT,
                new HttpEntity<>(validUpdateRequest()),
                ApiErrorResponse.class,
                persisted.getId()
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertErrorResponse(response.getBody(), "APPLICATION_INVALID_STATUS");
    }

    @Test
    void shouldReturnConflictWhenDeletingApplicationWithGeneratedOffer() {
        LoanApplication persisted = persistDraftApplication();
        setStatus(persisted.getId(), LoanApplicationStatus.OFFER_GENERATED);

        ResponseEntity<ApiErrorResponse> response = adminClient().exchange(
                "/applications/{id}",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                ApiErrorResponse.class,
                persisted.getId()
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertErrorResponse(response.getBody(), "APPLICATION_INVALID_STATUS");
    }

    @Test
    void shouldReturnUnauthorizedWithoutCredentials() {
        ResponseEntity<ApiErrorResponse> response = restTemplate.getForEntity(
                "/applications/{id}",
                ApiErrorResponse.class,
                UUID.randomUUID()
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertErrorResponse(response.getBody(), "UNAUTHORIZED");
        assertTrue(response.getHeaders().containsHeader(HttpHeaders.WWW_AUTHENTICATE));
    }

    @Test
    void shouldReturnForbiddenWhenUserDeletesApplication() {
        LoanApplication persisted = persistDraftApplication();

        ResponseEntity<ApiErrorResponse> response = userClient().exchange(
                "/applications/{id}",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                ApiErrorResponse.class,
                persisted.getId()
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertErrorResponse(response.getBody(), "FORBIDDEN");
        assertEquals(LoanApplicationStatus.DRAFT, jpaRepository.findById(persisted.getId()).orElseThrow().getStatus());
    }

    @Test
    void shouldReturnUnsupportedMediaTypeForNonJsonRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);

        ResponseEntity<ApiErrorResponse> response = userClient().exchange(
                "/applications",
                HttpMethod.POST,
                new HttpEntity<>("not-json", headers),
                ApiErrorResponse.class
        );

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
        assertErrorResponse(response.getBody(), "UNSUPPORTED_MEDIA_TYPE");
    }

    private KafkaConsumer<String, String> createKafkaConsumer() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "loan-application-integration-" + UUID.randomUUID());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(properties);
    }

    private ConsumerRecord<String, String> awaitRecord(KafkaConsumer<String, String> consumer, UUID applicationId) {
        Instant deadline = Instant.now().plusSeconds(10);

        while (Instant.now().isBefore(deadline)) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> record : records) {
                if (applicationId.toString().equals(record.key())) {
                    return record;
                }
            }
        }

        throw new AssertionError("No loan application event received for applicationId=" + applicationId);
    }

    private LoanApplication persistDraftApplication() {
        return domainRepository.save(new LoanApplication(
                CIF,
                BigDecimal.valueOf(42_000),
                60,
                "Integration test purpose"
        ));
    }

    private void setStatus(UUID applicationId, LoanApplicationStatus status) {
        int updatedRows = jdbcTemplate.update(
                "UPDATE loan_application SET status = ? WHERE id = ?",
                status.name(),
                applicationId
        );
        assertEquals(1, updatedRows);
    }

    private TestRestTemplate adminClient() {
        return restTemplate.withBasicAuth("admin", "admin123");
    }

    private TestRestTemplate userClient() {
        return restTemplate.withBasicAuth("user", "user123");
    }

    private static CreateLoanApplicationRequest validCreateRequest() {
        return new CreateLoanApplicationRequest(
                CIF,
                BigDecimal.valueOf(42_000),
                60,
                "Integration test purpose"
        );
    }

    private static UpdateLoanApplicationRequest validUpdateRequest() {
        return new UpdateLoanApplicationRequest(
                BigDecimal.valueOf(45_000),
                60,
                "Updated purpose"
        );
    }

    private static void assertErrorResponse(ApiErrorResponse response, String expectedCode) {
        assertNotNull(response);
        assertEquals(expectedCode, response.code());
        assertNotNull(response.message());
        assertNotNull(response.details());
        assertNotNull(response.correlationId());
        assertNotNull(response.timestamp());
    }
}
