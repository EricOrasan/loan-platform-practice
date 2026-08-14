package com.btproject.loanplatform.notification_service.infrastructure.messaging.in;

import com.btproject.loanplatform.notification_service.application.command.CreateNotificationCommand;
import com.btproject.loanplatform.notification_service.application.port.in.CreateNotificationUseCase;
import com.btproject.loanplatform.notification_service.infrastructure.messaging.in.exception.InvalidEventPayloadException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class LoanOfferGeneratedKafkaConsumer {

    private final CreateNotificationUseCase useCase;

    public LoanOfferGeneratedKafkaConsumer(CreateNotificationUseCase useCase) {
        this.useCase = useCase;
    }

    @KafkaListener(topics = "${app.kafka.topics.loan-offer-generated}")
    public void consume(LoanOfferGeneratedEvent event) {
        CreateNotificationCommand command = toCommand(event);
        useCase.create(command);
    }

    private CreateNotificationCommand toCommand(LoanOfferGeneratedEvent event) {
        validate(event);

        try {
            return new CreateNotificationCommand(
                    event.applicationId(),
                    event.cif()
            );
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidEventPayloadException("Invalid loan offer generated event", exception);
        }
    }

    private void validate(LoanOfferGeneratedEvent event) {
        if (event == null) {
            throw new InvalidEventPayloadException("Loan offer generated event must not be null");
        }

        if (event.eventId() == null) {
            throw new InvalidEventPayloadException("eventId must not be null");
        }

        if (!"LOAN_OFFER_GENERATED".equals(event.eventType())) {
            throw new InvalidEventPayloadException("Unexpected event type: " + event.eventType());
        }

        if (event.applicationId() == null) {
            throw new InvalidEventPayloadException("applicationId must not be null");
        }

        if (event.cif() == null || !event.cif().matches("[0-9]{8}")) {
            throw new InvalidEventPayloadException("CIF must contain exactly 8 digits");
        }

        if (event.amount() == null || event.amount().signum() <= 0) {
            throw new InvalidEventPayloadException("amount must be greater than 0");
        }

        if (event.periodMonths() == null || event.periodMonths() < 6 || event.periodMonths() > 120) {
            throw new InvalidEventPayloadException("periodMonths must be between 6 and 120");
        }

        if (event.interestRate() == null || event.interestRate().signum() <= 0) {
            throw new InvalidEventPayloadException("interestRate must be greater than 0");
        }

        if (event.monthlyInstallment() == null || event.monthlyInstallment().signum() <= 0) {
            throw new InvalidEventPayloadException("monthlyInstallment must be greater than 0");
        }

        if (event.createdAt() == null) {
            throw new InvalidEventPayloadException("createdAt must not be null");
        }
    }
}
