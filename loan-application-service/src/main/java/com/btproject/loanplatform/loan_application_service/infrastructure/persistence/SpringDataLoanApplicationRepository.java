package com.btproject.loanplatform.loan_application_service.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataLoanApplicationRepository extends JpaRepository<LoanApplicationJpaEntity, UUID> {
}
