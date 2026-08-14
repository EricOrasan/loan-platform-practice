package com.btproject.loanplatform.offer_service.infrastructure.messaging.in;

import com.btproject.loanplatform.offer_service.application.command.GenerateLoanOfferCommand;
import com.btproject.loanplatform.offer_service.application.port.in.GenerateLoanOfferUseCase;
import com.btproject.loanplatform.offer_service.domain.AssessmentDecision;
import com.btproject.loanplatform.offer_service.infrastructure.messaging.in.exception.InvalidEventPayloadException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class LoanAssessmentCompletedKafkaConsumer {

    private final GenerateLoanOfferUseCase useCase;

    public LoanAssessmentCompletedKafkaConsumer(GenerateLoanOfferUseCase useCase) {
        this.useCase = useCase;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.loan-assessment-completed}"
    )
    public void consume(LoanAssessmentCompletedEvent event) {
        GenerateLoanOfferCommand command = toCommand(event);
        useCase.generate(command);
    }

    private GenerateLoanOfferCommand toCommand(LoanAssessmentCompletedEvent event) {
        if (event == null) {
            throw new InvalidEventPayloadException("Loan assessment completed event must not be null");
        }

        if (!"LOAN_ASSESSMENT_COMPLETED".equals(event.eventType())) {
            throw new InvalidEventPayloadException("Unexpected event type: " + event.eventType());
        }

        if (event.cif() == null || !event.cif().matches("[0-9]{8}")) {
            throw new InvalidEventPayloadException("CIF must contain exactly 8 digits");
        }

        if (event.reason() == null || event.reason().isBlank()) {
            throw new InvalidEventPayloadException("Assessment reason must not be blank");
        }

        try {
            AssessmentDecision decision = AssessmentDecision.valueOf(event.decision());

            return new GenerateLoanOfferCommand(
                    event.eventId(),
                    event.applicationId(),
                    event.cif(),
                    event.requestedAmount(),
                    event.requestedPeriodMonths(),
                    event.score(),
                    decision,
                    event.createdAt()
            );
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidEventPayloadException("Invalid loan assessment completed event", exception);
        }
    }
}
