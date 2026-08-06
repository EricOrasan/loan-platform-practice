package com.btproject.loanplatform.credit_assessment_service.infrastructure.messaging.out;

import com.btproject.loanplatform.credit_assessment_service.application.port.out.CreditAssessmentEventPublisher;
import com.btproject.loanplatform.credit_assessment_service.domain.CreditAssessment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class KafkaCreditAssessmentEventPublisher implements CreditAssessmentEventPublisher {

    private final KafkaTemplate<String, LoanAssessmentCompletedEvent> kafkaTemplate;
    private final String topic;

    public KafkaCreditAssessmentEventPublisher(
            KafkaTemplate<String, LoanAssessmentCompletedEvent> kafkaTemplate,
            @Value("${app.kafka.topics.loan-assessment-completed}")
            String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publishCompleted(CreditAssessment assessment) {
        LoanAssessmentCompletedEvent event =
                new LoanAssessmentCompletedEvent(
                        UUID.randomUUID(),
                        "LOAN_ASSESSMENT_COMPLETED",
                        assessment.getApplicationId(),
                        assessment.getCif(),
                        assessment.getScore(),
                        assessment.getDecision(),
                        assessment.getReason(),
                        assessment.getCreatedAt()
                );

        kafkaTemplate.send(topic, assessment.getApplicationId().toString(), event);
    }
}