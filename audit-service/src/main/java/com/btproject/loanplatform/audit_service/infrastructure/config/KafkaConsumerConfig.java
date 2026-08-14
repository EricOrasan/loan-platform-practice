package com.btproject.loanplatform.audit_service.infrastructure.config;

import com.btproject.loanplatform.audit_service.infrastructure.messaging.in.exception.InvalidAuditEventException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Bean
    public CommonErrorHandler kafkaErrorHandler(
            KafkaTemplate<?, ?> kafkaTemplate,
            @Value("${app.kafka.topics.audit-events-dlq}")
            String dlqTopic
    ) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(dlqTopic, record.partition())
        );
        recoverer.setFailIfSendResultIsError(true);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(1_000L, 2L)
        );
        errorHandler.addNotRetryableExceptions(InvalidAuditEventException.class);

        errorHandler.setRetryListeners(new RetryListener() {

            @Override
            public void failedDelivery(
                    ConsumerRecord<?, ?> record,
                    Exception exception,
                    int deliveryAttempt
            ) {
                LOGGER.warn(
                        "Kafka audit processing failed: topic={}, partition={}, offset={}, attempt={}, error={}",
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        deliveryAttempt,
                        exception.getMessage()
                );
            }

            @Override
            public void recovered(ConsumerRecord<?, ?> record, Exception exception) {
                LOGGER.warn(
                        "Kafka message sent to audit DLQ: originalTopic={}, partition={}, offset={}, error={}",
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        exception.getMessage()
                );
            }

            @Override
            public void recoveryFailed(
                    ConsumerRecord<?, ?> record,
                    Exception originalException,
                    Exception recoveryException
            ) {
                LOGGER.error(
                        "Failed to send Kafka message to audit DLQ: topic={}, partition={}, offset={}",
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        recoveryException
                );
            }
        });

        return errorHandler;
    }
}
