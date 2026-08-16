package com.btproject.loanplatform.offer_service.integration;

import com.btproject.loanplatform.offer_service.TestcontainersConfiguration;
import com.btproject.loanplatform.offer_service.domain.LoanOfferStatus;
import com.btproject.loanplatform.offer_service.infrastructure.messaging.in.LoanAssessmentCompletedEvent;
import com.btproject.loanplatform.offer_service.infrastructure.messaging.out.LoanOfferGeneratedEvent;
import com.btproject.loanplatform.offer_service.infrastructure.persistence.LoanOfferJpaEntity;
import com.btproject.loanplatform.offer_service.infrastructure.persistence.SpringDataLoanOfferRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
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
class LoanOfferMessagingIntegrationTest {

    private static final String CIF = "12345678";

    @Autowired
    private SpringDataLoanOfferRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaContainer kafkaContainer;

    @Value("${app.kafka.topics.loan-assessment-completed}")
    private String inputTopic;

    @Value("${app.kafka.topics.loan-offer-generated}")
    private String outputTopic;

    @Value("${app.kafka.topics.loan-assessment-completed-dlq}")
    private String dlqTopic;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void shouldGeneratePersistAndPublishOfferForApprovedAssessment() throws Exception {
        UUID applicationId = UUID.randomUUID();

        try (KafkaConsumer<String, String> outputConsumer = createConsumer()) {
            outputConsumer.subscribe(List.of(outputTopic));
            publish(assessmentEvent(applicationId, UUID.randomUUID(), "APPROVED", 90));

            LoanOfferJpaEntity offer = awaitOffer(applicationId);
            assertEquals(0, offer.getAmount().compareTo(BigDecimal.valueOf(30_000)));
            assertEquals(48, offer.getPeriodMonths());
            assertEquals(0, offer.getInterestRate().compareTo(BigDecimal.valueOf(8.5)));
            assertEquals(LoanOfferStatus.GENERATED, offer.getStatus());
            assertNotNull(offer.getMonthlyInstallment());
            assertNotNull(offer.getCreatedAt());

            LoanOfferGeneratedEvent event = readOfferEvent(outputConsumer, applicationId);
            assertEquals(applicationId, event.applicationId());
            assertEquals(CIF, event.cif());
            assertEquals(0, event.amount().compareTo(BigDecimal.valueOf(30_000)));
            assertEquals(48, event.periodMonths());
            assertEquals(0, event.interestRate().compareTo(BigDecimal.valueOf(8.5)));
            assertEquals(0, event.monthlyInstallment().compareTo(offer.getMonthlyInstallment()));
            assertEquals("LOAN_OFFER_GENERATED", event.eventType());
            assertNotNull(event.eventId());
            assertNotNull(event.createdAt());
        }
    }

    @Test
    void shouldNotGenerateOfferForRejectedAssessment() throws Exception {
        UUID rejectedApplicationId = UUID.randomUUID();
        UUID approvedBarrierId = UUID.randomUUID();

        try (KafkaConsumer<String, String> outputConsumer = createConsumer()) {
            outputConsumer.subscribe(List.of(outputTopic));
            publish(assessmentEvent(rejectedApplicationId, UUID.randomUUID(), "REJECTED", 0));
            publish(assessmentEvent(approvedBarrierId, UUID.randomUUID(), "APPROVED", 90));

            awaitOffer(approvedBarrierId);
            readOfferEvent(outputConsumer, approvedBarrierId);

            assertNull(findOffer(rejectedApplicationId));
            assertEquals(1, repository.count());
            assertNull(awaitRecord(outputConsumer, rejectedApplicationId.toString(), Duration.ofSeconds(1)));
        }
    }

    @Test
    void shouldSendInvalidAssessmentEventToDlq() throws Exception {
        UUID messageKey = UUID.randomUUID();
        String invalidPayload = """
                {
                  "eventId": "%s",
                  "eventType": "LOAN_ASSESSMENT_COMPLETED",
                  "applicationId": null,
                  "cif": "12345678",
                  "requestedAmount": 30000,
                  "requestedPeriodMonths": 48,
                  "score": 90,
                  "decision": "APPROVED",
                  "reason": "CUSTOMER_ELIGIBLE",
                  "createdAt": "2026-08-16T10:00:00Z"
                }
                """.formatted(UUID.randomUUID());

        try (KafkaConsumer<String, String> dlqConsumer = createConsumer()) {
            dlqConsumer.subscribe(List.of(dlqTopic));
            publishRaw(messageKey.toString(), invalidPayload);

            ConsumerRecord<String, String> dlqRecord =
                    awaitRecord(dlqConsumer, messageKey.toString(), Duration.ofSeconds(10));

            assertNotNull(dlqRecord);
            assertEquals(objectMapper.readTree(invalidPayload), objectMapper.readTree(dlqRecord.value()));
            assertEquals(0, repository.count());
        }
    }

    @Test
    void shouldSendUnexpectedProcessingFailureToDlq() throws Exception {
        UUID applicationId = UUID.randomUUID();

        try (KafkaConsumer<String, String> dlqConsumer = createConsumer()) {
            dlqConsumer.subscribe(List.of(dlqTopic));
            publish(assessmentEvent(applicationId, UUID.randomUUID(), "APPROVED", 69));

            ConsumerRecord<String, String> dlqRecord =
                    awaitRecord(dlqConsumer, applicationId.toString(), Duration.ofSeconds(10));

            assertNotNull(dlqRecord);
            assertEquals(0, repository.count());
        }
    }

    @Test
    void shouldIgnoreDuplicateApprovedAssessment() throws Exception {
        UUID applicationId = UUID.randomUUID();

        try (KafkaConsumer<String, String> outputConsumer = createConsumer();
             KafkaConsumer<String, String> dlqConsumer = createConsumer()) {
            outputConsumer.subscribe(List.of(outputTopic));
            dlqConsumer.subscribe(List.of(dlqTopic));

            publish(assessmentEvent(applicationId, UUID.randomUUID(), "APPROVED", 90));
            publish(assessmentEvent(applicationId, UUID.randomUUID(), "APPROVED", 90));

            awaitOffer(applicationId);
            readOfferEvent(outputConsumer, applicationId);

            assertNull(awaitRecord(dlqConsumer, applicationId.toString(), Duration.ofSeconds(2)));
            assertEquals(1, repository.count());
        }
    }

    private void publish(LoanAssessmentCompletedEvent event) throws Exception {
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
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "offer-integration-" + UUID.randomUUID());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(properties);
    }

    private LoanOfferJpaEntity awaitOffer(UUID applicationId) {
        Instant deadline = Instant.now().plusSeconds(12);

        while (Instant.now().isBefore(deadline)) {
            LoanOfferJpaEntity offer = findOffer(applicationId);
            if (offer != null) {
                return offer;
            }
            pause(Duration.ofMillis(100));
        }

        throw new AssertionError("No offer persisted for applicationId=" + applicationId);
    }

    private LoanOfferJpaEntity findOffer(UUID applicationId) {
        return repository.findAll().stream()
                .filter(offer -> applicationId.equals(offer.getApplicationId()))
                .findFirst()
                .orElse(null);
    }

    private LoanOfferGeneratedEvent readOfferEvent(
            KafkaConsumer<String, String> consumer,
            UUID applicationId
    ) throws Exception {
        ConsumerRecord<String, String> record =
                awaitRecord(consumer, applicationId.toString(), Duration.ofSeconds(10));
        assertNotNull(record, "No offer event received for applicationId=" + applicationId);
        return objectMapper.readValue(record.value(), LoanOfferGeneratedEvent.class);
    }

    private ConsumerRecord<String, String> awaitRecord(
            KafkaConsumer<String, String> consumer,
            String key,
            Duration timeout
    ) {
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

    private static LoanAssessmentCompletedEvent assessmentEvent(
            UUID applicationId,
            UUID eventId,
            String decision,
            int score
    ) {
        return new LoanAssessmentCompletedEvent(
                eventId,
                "LOAN_ASSESSMENT_COMPLETED",
                applicationId,
                CIF,
                BigDecimal.valueOf(30_000),
                48,
                score,
                decision,
                "ASSESSMENT_REASON",
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
