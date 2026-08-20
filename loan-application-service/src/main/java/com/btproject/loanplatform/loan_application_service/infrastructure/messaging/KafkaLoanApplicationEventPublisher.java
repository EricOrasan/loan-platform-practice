package com.btproject.loanplatform.loan_application_service.infrastructure.messaging;

import com.btproject.loanplatform.loan_application_service.application.exception.EventPublishingUnavailableException;
import com.btproject.loanplatform.loan_application_service.application.port.out.LoanApplicationEventPublisher;
import com.btproject.loanplatform.loan_application_service.domain.LoanApplication;
import org.apache.kafka.common.errors.RetriableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class KafkaLoanApplicationEventPublisher implements LoanApplicationEventPublisher {

    private final KafkaTemplate<String, LoanApplicationCreatedEvent> kafkaTemplate;
    private final String topic;
    private final long publishTimeoutSeconds;

    public KafkaLoanApplicationEventPublisher(
            KafkaTemplate<String, LoanApplicationCreatedEvent> kafkaTemplate,
            @Value("${app.kafka.topics.loan-application-created}")
            String topic,
            @Value("${app.kafka.publish-timeout-seconds}")
            long publishTimeoutSeconds
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.publishTimeoutSeconds = publishTimeoutSeconds;
    }

    @Override
    public void publishCreated(LoanApplication loanApplication) {

        LoanApplicationCreatedEvent createdEvent = new LoanApplicationCreatedEvent(
                UUID.randomUUID(),
                "LOAN_APPLICATION_CREATED",
                loanApplication.getId(),
                loanApplication.getCif(),
                loanApplication.getRequestedAmount(),
                loanApplication.getRequestedPeriodMonths(),
                loanApplication.getCreatedAt()
        );

        try {
            kafkaTemplate.send(topic, loanApplication.getId().toString(), createdEvent).get(publishTimeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException exception) {
            throw new EventPublishingUnavailableException("Kafka publishing timed out", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Event publishing was interrupted", exception);
        } catch (ExecutionException execution) {
            Throwable cause = execution.getCause();
            if (isTemporaryFailure(cause)) {
                throw new EventPublishingUnavailableException("Kafka is temporarily unavailable", cause);
            }
            throw new IllegalStateException("Unexpected event publishing error", cause);
        } catch (RuntimeException exception) {
            if (isTemporaryFailure(exception)) {
                throw new EventPublishingUnavailableException("Kafka is temporarily unavailable", exception);
            }

            throw new IllegalStateException("Unexpected event publishing error", exception);
        }
    }

    private boolean isTemporaryFailure(Throwable throwable) {
        Throwable current = throwable;

        while (current != null) {
            if (current instanceof RetriableException || current instanceof TimeoutException) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}
