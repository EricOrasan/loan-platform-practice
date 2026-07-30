package com.btproject.loanplatform.loan_application_service.infrastructure.persistence;

import com.btproject.loanplatform.loan_application_service.domain.LoanApplication;
import org.springframework.stereotype.Component;

@Component
public class LoanApplicationPersistenceMapper {

    public LoanApplicationJpaEntity toJpaEntity(LoanApplication loanApplication) {
        return new LoanApplicationJpaEntity(
                loanApplication.getId(),
                loanApplication.getApplicationNumber(),
                loanApplication.getCif(),
                loanApplication.getRequestedAmount(),
                loanApplication.getRequestedPeriodMonths(),
                loanApplication.getPurpose(),
                loanApplication.getStatus(),
                loanApplication.getCreatedAt(),
                loanApplication.getUpdatedAt()
        );
    }

    public LoanApplication toDomain(LoanApplicationJpaEntity loanApplicationJpaEntity) {
        return LoanApplication.restore(
                loanApplicationJpaEntity.getId(),
                loanApplicationJpaEntity.getApplicationNumber(),
                loanApplicationJpaEntity.getCif(),
                loanApplicationJpaEntity.getRequestedAmount(),
                loanApplicationJpaEntity.getRequestedPeriodMonths(),
                loanApplicationJpaEntity.getPurpose(),
                loanApplicationJpaEntity.getStatus(),
                loanApplicationJpaEntity.getCreatedAt(),
                loanApplicationJpaEntity.getUpdatedAt()
        );
    }
}
