package com.btproject.loanplatform.credit_assessment_service.application.port.out;

import com.btproject.loanplatform.credit_assessment_service.domain.CreditAssessment;

import java.util.UUID;

public interface CreditAssessmentRepository {
    boolean existsByApplicationId(UUID applicationId);
    CreditAssessment save(CreditAssessment assessment);
}
