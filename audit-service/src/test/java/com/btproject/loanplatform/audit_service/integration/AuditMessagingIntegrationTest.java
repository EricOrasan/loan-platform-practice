package com.btproject.loanplatform.audit_service.integration;

import com.btproject.loanplatform.audit_service.TestcontainersConfiguration;
import com.btproject.loanplatform.audit_service.domain.AuditEventType;
import com.btproject.loanplatform.audit_service.infrastructure.persistence.AuditEventJpaEntity;
import com.btproject.loanplatform.audit_service.infrastructure.persistence.SpringDataAuditEventRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AuditMessagingIntegrationTest {

    @Autowired SpringDataAuditEventRepository repository;
    @Autowired KafkaContainer kafka;
    @Value("${app.kafka.topics.loan-application-created}") String applicationTopic;
    @Value("${app.kafka.topics.loan-assessment-completed}") String assessmentTopic;
    @Value("${app.kafka.topics.loan-offer-generated}") String offerTopic;
    @Value("${app.kafka.topics.audit-events-dlq}") String dlqTopic;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @ParameterizedTest
    @EnumSource(AuditEventType.class)
    void shouldPersistEverySupportedEventType(AuditEventType type) throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        String payload = validPayload(eventId, type, applicationId);

        publish(topic(type), applicationId.toString(), payload);

        AuditEventJpaEntity saved = awaitEvent(eventId);
        assertEquals(type, saved.getEventType());
        assertEquals(applicationId, saved.getAggregateId());
        assertEquals(payload, saved.getPayload());
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void shouldIgnoreDuplicateEventId() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        String payload = validPayload(eventId, AuditEventType.LOAN_APPLICATION_CREATED, applicationId);

        try (KafkaConsumer<String, String> dlq = consumer()) {
            dlq.subscribe(List.of(dlqTopic));
            publish(applicationTopic, applicationId.toString(), payload);
            publish(applicationTopic, applicationId.toString(), payload);
            awaitEvent(eventId);
            awaitBarrier(dlq);
        }

        assertEquals(1, repository.count());
    }

    @Test
    void shouldSendMalformedJsonToDlq() throws Exception {
        assertSentToDlq("{not-json");
    }

    @Test
    void shouldSendUnsupportedEventTypeToDlq() throws Exception {
        assertSentToDlq("""
                {"eventId":"%s","eventType":"UNKNOWN_EVENT","applicationId":"%s"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void shouldSendMissingApplicationIdToDlq() throws Exception {
        assertSentToDlq("""
                {"eventId":"%s","eventType":"LOAN_APPLICATION_CREATED"}
                """.formatted(UUID.randomUUID()));
    }

    private void assertSentToDlq(String payload) throws Exception {
        UUID key = UUID.randomUUID();
        try (KafkaConsumer<String, String> dlq = consumer()) {
            dlq.subscribe(List.of(dlqTopic));
            publish(applicationTopic, key.toString(), payload);
            ConsumerRecord<String, String> record = awaitRecord(dlq, key.toString());
            assertNotNull(record);
            assertEquals(payload, record.value());
            assertEquals(0, repository.count());
        }
    }

    private void awaitBarrier(KafkaConsumer<String, String> dlq) throws Exception {
        UUID key = UUID.randomUUID();
        publish(applicationTopic, key.toString(), "{invalid-barrier");
        assertNotNull(awaitRecord(dlq, key.toString()));
    }

    private void publish(String topic, String key, String payload) throws Exception {
        Properties config = new Properties();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(config)) {
            producer.send(new ProducerRecord<>(topic, key, payload)).get(10, TimeUnit.SECONDS);
        }
    }

    private KafkaConsumer<String, String> consumer() {
        Properties config = new Properties();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "audit-integration-" + UUID.randomUUID());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(config);
    }

    private AuditEventJpaEntity awaitEvent(UUID eventId) {
        Instant deadline = Instant.now().plusSeconds(12);
        while (Instant.now().isBefore(deadline)) {
            AuditEventJpaEntity found = repository.findAll().stream()
                    .filter(event -> eventId.equals(event.getEventId()))
                    .findFirst().orElse(null);
            if (found != null) return found;
            pause();
        }
        throw new AssertionError("No audit event persisted for eventId=" + eventId);
    }

    private ConsumerRecord<String, String> awaitRecord(KafkaConsumer<String, String> consumer, String key) {
        Instant deadline = Instant.now().plusSeconds(10);
        while (Instant.now().isBefore(deadline)) {
            for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(250))) {
                if (key.equals(record.key())) return record;
            }
        }
        return null;
    }

    private String topic(AuditEventType type) {
        return switch (type) {
            case LOAN_APPLICATION_CREATED -> applicationTopic;
            case LOAN_ASSESSMENT_COMPLETED -> assessmentTopic;
            case LOAN_OFFER_GENERATED -> offerTopic;
        };
    }

    private static String validPayload(UUID eventId, AuditEventType type, UUID applicationId) {
        return """
                {"eventId":"%s","eventType":"%s","applicationId":"%s","createdAt":"2026-08-16T10:00:00Z"}
                """.formatted(eventId, type, applicationId);
    }

    private static void pause() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Test wait interrupted", exception);
        }
    }
}
