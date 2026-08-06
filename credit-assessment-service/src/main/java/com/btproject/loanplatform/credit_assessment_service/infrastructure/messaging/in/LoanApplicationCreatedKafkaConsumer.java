package com.btproject.loanplatform.credit_assessment_service.infrastructure.messaging.in;

import com.btproject.loanplatform.credit_assessment_service.application.command.ProcessLoanApplicationCommand;
import com.btproject.loanplatform.credit_assessment_service.application.port.in.ProcessLoanApplicationUseCase;
import com.btproject.loanplatform.credit_assessment_service.infrastructure.messaging.in.exception.InvalidEventPayloadException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class LoanApplicationCreatedKafkaConsumer {

    private final ProcessLoanApplicationUseCase useCase;

    public LoanApplicationCreatedKafkaConsumer(ProcessLoanApplicationUseCase useCase) {
        this.useCase = useCase;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.loan-application-created}"
    )
    public void consume(LoanApplicationCreatedEvent event) {
        ProcessLoanApplicationCommand command = toCommand(event);
        useCase.process(command);
    }

    private ProcessLoanApplicationCommand toCommand(LoanApplicationCreatedEvent event) {

        if (event == null) {
            throw new InvalidEventPayloadException("Loan application created event must not be null");
        }

        if (!"LOAN_APPLICATION_CREATED".equals(event.eventType())) {
            throw new InvalidEventPayloadException("Unexpected event type: " + event.eventType());
        }

        try {
            return new ProcessLoanApplicationCommand(
                    event.eventId(),
                    event.applicationId(),
                    event.cif(),
                    event.requestedAmount(),
                    event.requestedPeriodMonths(),
                    event.createdAt()
            );
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidEventPayloadException("Invalid loan application created event", exception);
        }
    }
}
