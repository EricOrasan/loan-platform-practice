package com.btproject.loanplatform.credit_assessment_service.application.port.out;

import com.btproject.loanplatform.credit_assessment_service.domain.CreditAssessment;

import java.math.BigDecimal;

public interface CreditAssessmentEventPublisher {

    void publishCompleted(
            CreditAssessment assessment,
            BigDecimal requestedAmount,
            int requestedPeriodMonths
    );
}