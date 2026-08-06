package com.btproject.loanplatform.credit_assessment_service.infrastructure.persistence;

import com.btproject.loanplatform.credit_assessment_service.domain.CreditAssessment;
import org.springframework.stereotype.Component;

@Component
public class CreditAssessmentPersistenceMapper {

    public CreditAssessmentJpaEntity toJpaEntity(CreditAssessment creditAssessment) {
        return new CreditAssessmentJpaEntity(
                creditAssessment.getId(),
                creditAssessment.getApplicationId(),
                creditAssessment.getCif(),
                creditAssessment.getScore(),
                creditAssessment.getDecision(),
                creditAssessment.getReason(),
                creditAssessment.getCreatedAt()
        );
    }

    public CreditAssessment toDomain(CreditAssessmentJpaEntity creditAssessmentJpaEntity) {
        return CreditAssessment.restore(
                creditAssessmentJpaEntity.getId(),
                creditAssessmentJpaEntity.getApplicationId(),
                creditAssessmentJpaEntity.getCif(),
                creditAssessmentJpaEntity.getScore(),
                creditAssessmentJpaEntity.getDecision(),
                creditAssessmentJpaEntity.getReason(),
                creditAssessmentJpaEntity.getCreatedAt()
        );
    }
}
