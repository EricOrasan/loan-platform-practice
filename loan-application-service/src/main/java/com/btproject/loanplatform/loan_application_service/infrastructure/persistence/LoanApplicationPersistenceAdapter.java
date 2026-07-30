package com.btproject.loanplatform.loan_application_service.infrastructure.persistence;

import com.btproject.loanplatform.loan_application_service.application.port.out.LoanApplicationRepository;
import com.btproject.loanplatform.loan_application_service.domain.LoanApplication;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class LoanApplicationPersistenceAdapter implements LoanApplicationRepository {

    private final SpringDataLoanApplicationRepository repository;
    private final LoanApplicationPersistenceMapper mapper;

    public LoanApplicationPersistenceAdapter(SpringDataLoanApplicationRepository repository, LoanApplicationPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public LoanApplication save(LoanApplication loanApplication) {
        LoanApplicationJpaEntity jpaEntity = mapper.toJpaEntity(loanApplication);
        LoanApplicationJpaEntity savedEntity = repository.save(jpaEntity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<LoanApplication> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }
}
