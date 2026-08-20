package com.btproject.loanplatform.credit_assessment_service.integration;

import com.btproject.loanplatform.credit_assessment_service.TestcontainersConfiguration;
import com.btproject.loanplatform.credit_assessment_service.domain.AssessmentDecision;
import com.btproject.loanplatform.credit_assessment_service.domain.AssessmentReason;
import com.btproject.loanplatform.credit_assessment_service.infrastructure.messaging.in.LoanApplicationCreatedEvent;
import com.btproject.loanplatform.credit_assessment_service.infrastructure.messaging.out.LoanAssessmentCompletedEvent;
import com.btproject.loanplatform.credit_assessment_service.infrastructure.persistence.CreditAssessmentJpaEntity;
import com.btproject.loanplatform.credit_assessment_service.infrastructure.persistence.SpringDataCreditAssessmentRepository;
import com.btproject.loanplatform.credit_assessment_service.integration.support.CustomerServiceStub;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CreditAssessmentMessagingIntegrationTest {

    private static final String CIF = "12345678";
    private static final String CUSTOMER_JSON = """
            {
              "cif": "12345678",
              "monthlyIncome": 8500,
              "riskCategory": "LOW"
            }
            """;
    private static final CustomerServiceStub CUSTOMER_SERVICE = new CustomerServiceStub();

    @Autowired
    private SpringDataCreditAssessmentRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaContainer kafkaContainer;

    @Value("${app.kafka.topics.loan-application-created}")
    private String inputTopic;

    @Value("${app.kafka.topics.loan-assessment-completed}")
    private String outputTopic;

    @Value("${app.kafka.topics.loan-application-created-dlq}")
    private String dlqTopic;

    @DynamicPropertySource
    static void customerServiceProperties(DynamicPropertyRegistry registry) {
        registry.add("app.clients.customer-service.base-url", CUSTOMER_SERVICE::baseUrl);
    }

    @BeforeEach
    void resetState() {
        repository.deleteAll();
        CUSTOMER_SERVICE.reset();
    }

    @AfterAll
    static void stopCustomerService() {
        CUSTOMER_SERVICE.close();
    }

    @Test
    void shouldConsumeAssessPersistAndPublishApprovedResult() throws Exception {
        CUSTOMER_SERVICE.respond(200, CUSTOMER_JSON);
        UUID applicationId = UUID.randomUUID();

        try (KafkaConsumer<String, String> outputConsumer = createConsumer()) {
            outputConsumer.subscribe(List.of(outputTopic));
            publish(validEvent(applicationId, UUID.randomUUID()));

            CreditAssessmentJpaEntity assessment = awaitAssessment(applicationId);
            assertEquals(90, assessment.getScore());
            assertEquals(AssessmentDecision.APPROVED, assessment.getDecision());
            assertEquals(AssessmentReason.CUSTOMER_ELIGIBLE, assessment.getReason());
            assertEquals(1, CUSTOMER_SERVICE.requestCount());

            LoanAssessmentCompletedEvent event = readAssessmentEvent(outputConsumer, applicationId);
            assertEquals(applicationId, event.applicationId());
            assertEquals(CIF, event.cif());
            assertEquals(0, event.requestedAmount().compareTo(BigDecimal.valueOf(30_000)));
            assertEquals(48, event.requestedPeriodMonths());
            assertEquals(90, event.score());
            assertEquals(AssessmentDecision.APPROVED, event.decision());
            assertEquals(AssessmentReason.CUSTOMER_ELIGIBLE, event.reason());
        }
    }

    @Test
    void shouldStoreRejectedAssessmentWhenCustomerIsNotFound() throws Exception {
        CUSTOMER_SERVICE.respond(404, "{}");
        UUID applicationId = UUID.randomUUID();

        try (KafkaConsumer<String, String> outputConsumer = createConsumer()) {
            outputConsumer.subscribe(List.of(outputTopic));
            publish(validEvent(applicationId, UUID.randomUUID()));

            CreditAssessmentJpaEntity assessment = awaitAssessment(applicationId);
            assertEquals(0, assessment.getScore());
            assertEquals(AssessmentDecision.REJECTED, assessment.getDecision());
            assertEquals(AssessmentReason.CUSTOMER_NOT_FOUND, assessment.getReason());
            assertEquals(1, CUSTOMER_SERVICE.requestCount());

            LoanAssessmentCompletedEvent event = readAssessmentEvent(outputConsumer, applicationId);
            assertEquals(AssessmentDecision.REJECTED, event.decision());
            assertEquals(AssessmentReason.CUSTOMER_NOT_FOUND, event.reason());
        }
    }

    @Test
    void shouldStoreManualReviewAfterHttpRetriesAreExhausted() throws Exception {
        CUSTOMER_SERVICE.respond(503, "{}");
        UUID applicationId = UUID.randomUUID();

        try (KafkaConsumer<String, String> outputConsumer = createConsumer()) {
            outputConsumer.subscribe(List.of(outputTopic));
            publish(validEvent(applicationId, UUID.randomUUID()));

            CreditAssessmentJpaEntity assessment = awaitAssessment(applicationId);
            assertEquals(0, assessment.getScore());
            assertEquals(AssessmentDecision.MANUAL_REVIEW, assessment.getDecision());
            assertEquals(AssessmentReason.TECHNICAL_PROCESSING_FAILED, assessment.getReason());
            assertEquals(3, CUSTOMER_SERVICE.requestCount());

            LoanAssessmentCompletedEvent event = readAssessmentEvent(outputConsumer, applicationId);
            assertEquals(AssessmentDecision.MANUAL_REVIEW, event.decision());
            assertEquals(AssessmentReason.TECHNICAL_PROCESSING_FAILED, event.reason());
        }
    }

    @Test
    void shouldSendInvalidEventToDlqWithoutPersistingAssessment() throws Exception {
        UUID messageKey = UUID.randomUUID();
        String invalidPayload = """
                {
                  "eventId": "%s",
                  "eventType": "LOAN_APPLICATION_CREATED",
                  "applicationId": null,
                  "cif": "12345678",
                  "requestedAmount": 30000,
                  "requestedPeriodMonths": 48,
                  "createdAt": "2026-08-16T10:00:00Z"
                }
                """.formatted(UUID.randomUUID());

        try (KafkaConsumer<String, String> dlqConsumer = createConsumer()) {
            dlqConsumer.subscribe(List.of(dlqTopic));
            publishRaw(messageKey.toString(), invalidPayload);

            ConsumerRecord<String, String> dlqRecord = awaitRecord(dlqConsumer, messageKey.toString(), Duration.ofSeconds(10));
            assertNotNull(dlqRecord);
            assertEquals(objectMapper.readTree(invalidPayload), objectMapper.readTree(dlqRecord.value()));
            assertEquals(0, repository.count());
            assertEquals(0, CUSTOMER_SERVICE.requestCount());
        }
    }

    @Test
    void shouldIgnoreDuplicateApplicationEvent() throws Exception {
        CUSTOMER_SERVICE.respond(200, CUSTOMER_JSON);
        UUID applicationId = UUID.randomUUID();

        try (KafkaConsumer<String, String> outputConsumer = createConsumer();
             KafkaConsumer<String, String> dlqConsumer = createConsumer()) {
            outputConsumer.subscribe(List.of(outputTopic));
            dlqConsumer.subscribe(List.of(dlqTopic));

            publish(validEvent(applicationId, UUID.randomUUID()));
            publish(validEvent(applicationId, UUID.randomUUID()));

            awaitAssessment(applicationId);
            readAssessmentEvent(outputConsumer, applicationId);

            ConsumerRecord<String, String> duplicateFailure = awaitRecord(dlqConsumer, applicationId.toString(), Duration.ofSeconds(2));

            assertNull(duplicateFailure);
            assertEquals(1, repository.count());
            assertEquals(1, CUSTOMER_SERVICE.requestCount());
        }
    }

    private void publish(LoanApplicationCreatedEvent event) throws Exception {
        publishRaw(event.applicationId().toString(), objectMapper.writeValueAsString(event));
    }

    private void publishRaw(String key, String payload) throws Exception {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(properties)) {
            producer.send(new ProducerRecord<>(inputTopic, key, payload)).get(10, TimeUnit.SECONDS);
        }
    }

    private KafkaConsumer<String, String> createConsumer() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "credit-assessment-integration-" + UUID.randomUUID());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(properties);
    }

    private CreditAssessmentJpaEntity awaitAssessment(UUID applicationId) {
        Instant deadline = Instant.now().plusSeconds(12);

        while (Instant.now().isBefore(deadline)) {
            for (CreditAssessmentJpaEntity assessment : repository.findAll()) {
                if (applicationId.equals(assessment.getApplicationId())) {
                    return assessment;
                }
            }

            pause(Duration.ofMillis(100));
        }

        throw new AssertionError("No assessment persisted for applicationId=" + applicationId);
    }

    private LoanAssessmentCompletedEvent readAssessmentEvent(KafkaConsumer<String, String> consumer, UUID applicationId) throws Exception {
        ConsumerRecord<String, String> record = awaitRecord(consumer, applicationId.toString(), Duration.ofSeconds(10));
        assertNotNull(record, "No assessment event received for applicationId=" + applicationId);
        return objectMapper.readValue(record.value(), LoanAssessmentCompletedEvent.class);
    }

    private ConsumerRecord<String, String> awaitRecord(KafkaConsumer<String, String> consumer, String key, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);

        while (Instant.now().isBefore(deadline)) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(250));
            for (ConsumerRecord<String, String> record : records) {
                if (key.equals(record.key())) {
                    return record;
                }
            }
        }

        return null;
    }

    private static LoanApplicationCreatedEvent validEvent(UUID applicationId, UUID eventId) {
        return new LoanApplicationCreatedEvent(
                eventId,
                "LOAN_APPLICATION_CREATED",
                applicationId,
                CIF,
                BigDecimal.valueOf(30_000),
                48,
                Instant.now()
        );
    }

    private static void pause(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Test wait was interrupted", exception);
        }
    }
}
