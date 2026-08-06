package com.btproject.loanplatform.credit_assessment_service.infrastructure.persistence;

import com.btproject.loanplatform.credit_assessment_service.application.port.out.CreditAssessmentRepository;
import com.btproject.loanplatform.credit_assessment_service.domain.CreditAssessment;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class CreditAssessmentRepositoryAdapter implements CreditAssessmentRepository {

    private final SpringDataCreditAssessmentRepository repository;
    private final CreditAssessmentPersistenceMapper mapper;

    public CreditAssessmentRepositoryAdapter(SpringDataCreditAssessmentRepository repository, CreditAssessmentPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public boolean existsByApplicationId(UUID applicationId) {
        return repository.existsByApplicationId(applicationId);
    }

    @Override
    public CreditAssessment save(CreditAssessment assessment) {
        CreditAssessmentJpaEntity entity = mapper.toJpaEntity(assessment);
        CreditAssessmentJpaEntity savedEntity = repository.save(entity);
        return mapper.toDomain(savedEntity);
    }
}
