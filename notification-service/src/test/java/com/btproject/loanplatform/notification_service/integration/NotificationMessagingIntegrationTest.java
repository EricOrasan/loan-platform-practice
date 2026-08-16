package com.btproject.loanplatform.notification_service.integration;

import com.btproject.loanplatform.notification_service.TestcontainersConfiguration;
import com.btproject.loanplatform.notification_service.domain.NotificationChannel;
import com.btproject.loanplatform.notification_service.domain.NotificationStatus;
import com.btproject.loanplatform.notification_service.infrastructure.messaging.in.LoanOfferGeneratedEvent;
import com.btproject.loanplatform.notification_service.infrastructure.persistence.NotificationJpaEntity;
import com.btproject.loanplatform.notification_service.infrastructure.persistence.SpringDataNotificationRepository;
import com.btproject.loanplatform.notification_service.integration.support.CustomerServiceStub;
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
class NotificationMessagingIntegrationTest {

    private static final String CIF = "12345678";
    private static final String EMAIL = "andrei.popescu@example.com";
    private static final String CONTACT_JSON = """
            {
              "cif": "12345678",
              "email": "andrei.popescu@example.com"
            }
            """;
    private static final CustomerServiceStub CUSTOMER_SERVICE = new CustomerServiceStub();

    @Autowired
    private SpringDataNotificationRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaContainer kafkaContainer;

    @Value("${app.kafka.topics.loan-offer-generated}")
    private String inputTopic;

    @Value("${app.kafka.topics.loan-offer-generated-dlq}")
    private String dlqTopic;

    @DynamicPropertySource
    static void customerServiceProperties(DynamicPropertyRegistry registry) {
        registry.add("app.clients.customer-service.base-url", CUSTOMER_SERVICE::baseUrl);
        registry.add("app.clients.customer-service.retry.max-attempts", () -> 2);
        registry.add("app.clients.customer-service.retry.backoff", () -> "0ms");
    }

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        CUSTOMER_SERVICE.reset();
    }

    @Test
    void shouldCreateSendAndPersistNotificationForValidOfferEvent() throws Exception {
        UUID applicationId = UUID.randomUUID();
        CUSTOMER_SERVICE.respond(200, CONTACT_JSON);

        publish(offerEvent(applicationId, UUID.randomUUID()));

        NotificationJpaEntity notification = awaitNotification(applicationId);
        assertEquals(NotificationChannel.EMAIL, notification.getChannel());
        assertEquals(EMAIL, notification.getRecipient());
        assertEquals(NotificationStatus.SENT, notification.getStatus());
        assertEquals(
                "Your loan offer was generated successfully for application " + applicationId + ".",
                notification.getMessage()
        );
        assertNotNull(notification.getId());
        assertNotNull(notification.getCreatedAt());
        assertEquals(1, CUSTOMER_SERVICE.requestCount());
        assertEquals("Basic dXNlcjp1c2VyMTIz", CUSTOMER_SERVICE.authorizationHeader());
    }

    @Test
    void shouldIgnoreDuplicateOfferEvent() throws Exception {
        UUID applicationId = UUID.randomUUID();
        CUSTOMER_SERVICE.respond(200, CONTACT_JSON);

        publish(offerEvent(applicationId, UUID.randomUUID()));
        awaitNotification(applicationId);
        publish(offerEvent(applicationId, UUID.randomUUID()));
        awaitConsumerBarrier();

        assertEquals(1, repository.count());
        assertEquals(1, CUSTOMER_SERVICE.requestCount());
    }

    @Test
    void shouldSendInvalidOfferEventToDlq() throws Exception {
        UUID key = UUID.randomUUID();
        String invalidPayload = """
                {
                  "eventId": "%s",
                  "eventType": "LOAN_OFFER_GENERATED",
                  "applicationId": null,
                  "cif": "12345678",
                  "amount": 30000,
                  "periodMonths": 48,
                  "interestRate": 8.5,
                  "monthlyInstallment": 739.38,
                  "createdAt": "2026-08-16T10:00:00Z"
                }
                """.formatted(UUID.randomUUID());

        try (KafkaConsumer<String, String> dlqConsumer = createConsumer()) {
            dlqConsumer.subscribe(List.of(dlqTopic));
            publishRaw(key.toString(), invalidPayload);

            ConsumerRecord<String, String> dlqRecord =
                    awaitRecord(dlqConsumer, key.toString(), Duration.ofSeconds(10));

            assertNotNull(dlqRecord);
            assertEquals(objectMapper.readTree(invalidPayload), objectMapper.readTree(dlqRecord.value()));
            assertEquals(0, repository.count());
            assertEquals(0, CUSTOMER_SERVICE.requestCount());
        }
    }

    @Test
    void shouldSendMissingCustomerContactToDlq() throws Exception {
        UUID applicationId = UUID.randomUUID();
        CUSTOMER_SERVICE.respond(404, "{}");

        try (KafkaConsumer<String, String> dlqConsumer = createConsumer()) {
            dlqConsumer.subscribe(List.of(dlqTopic));
            publish(offerEvent(applicationId, UUID.randomUUID()));

            ConsumerRecord<String, String> dlqRecord =
                    awaitRecord(dlqConsumer, applicationId.toString(), Duration.ofSeconds(10));

            assertNotNull(dlqRecord);
            assertEquals(0, repository.count());
            assertEquals(1, CUSTOMER_SERVICE.requestCount());
        }
    }

    @Test
    void shouldRetryUnavailableCustomerServiceAndSendEventToDlq() throws Exception {
        UUID applicationId = UUID.randomUUID();
        CUSTOMER_SERVICE.respond(503, "{}");

        try (KafkaConsumer<String, String> dlqConsumer = createConsumer()) {
            dlqConsumer.subscribe(List.of(dlqTopic));
            publish(offerEvent(applicationId, UUID.randomUUID()));

            ConsumerRecord<String, String> dlqRecord =
                    awaitRecord(dlqConsumer, applicationId.toString(), Duration.ofSeconds(10));

            assertNotNull(dlqRecord);
            assertEquals(0, repository.count());
            assertEquals(2, CUSTOMER_SERVICE.requestCount());
        }
    }

    private void publish(LoanOfferGeneratedEvent event) throws Exception {
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
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-integration-" + UUID.randomUUID());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(properties);
    }

    private NotificationJpaEntity awaitNotification(UUID applicationId) {
        Instant deadline = Instant.now().plusSeconds(12);

        while (Instant.now().isBefore(deadline)) {
            NotificationJpaEntity notification = findNotification(applicationId);
            if (notification != null) {
                return notification;
            }
            pause(Duration.ofMillis(100));
        }

        throw new AssertionError("No notification persisted for applicationId=" + applicationId);
    }

    private NotificationJpaEntity findNotification(UUID applicationId) {
        return repository.findAll().stream()
                .filter(notification -> applicationId.equals(notification.getApplicationId()))
                .findFirst()
                .orElse(null);
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

    private void awaitConsumerBarrier() throws Exception {
        UUID barrierId = UUID.randomUUID();
        String invalidBarrier = """
                {
                  "eventId": "%s",
                  "eventType": "LOAN_OFFER_GENERATED",
                  "applicationId": null
                }
                """.formatted(UUID.randomUUID());

        try (KafkaConsumer<String, String> dlqConsumer = createConsumer()) {
            dlqConsumer.subscribe(List.of(dlqTopic));
            publishRaw(barrierId.toString(), invalidBarrier);
            assertNotNull(awaitRecord(dlqConsumer, barrierId.toString(), Duration.ofSeconds(10)));
        }
    }

    private static LoanOfferGeneratedEvent offerEvent(UUID applicationId, UUID eventId) {
        return new LoanOfferGeneratedEvent(
                eventId,
                "LOAN_OFFER_GENERATED",
                applicationId,
                CIF,
                BigDecimal.valueOf(30_000),
                48,
                BigDecimal.valueOf(8.5),
                BigDecimal.valueOf(739.38),
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
