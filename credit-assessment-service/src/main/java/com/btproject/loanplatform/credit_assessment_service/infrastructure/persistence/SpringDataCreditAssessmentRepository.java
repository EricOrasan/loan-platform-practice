package com.btproject.loanplatform.credit_assessment_service.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataCreditAssessmentRepository extends JpaRepository<CreditAssessmentJpaEntity, UUID> {
    boolean existsByApplicationId(UUID applicationId);
}
