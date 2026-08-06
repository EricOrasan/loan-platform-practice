package com.btproject.loanplatform.credit_assessment_service.application.port.out;

import com.btproject.loanplatform.credit_assessment_service.domain.CreditAssessment;

public interface CreditAssessmentEventPublisher {
    void publishCompleted(CreditAssessment assessment);
}