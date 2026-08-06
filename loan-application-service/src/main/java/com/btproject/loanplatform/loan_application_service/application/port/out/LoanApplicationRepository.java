package com.btproject.loanplatform.loan_application_service.application.port.out;

import com.btproject.loanplatform.loan_application_service.domain.LoanApplication;

import java.util.Optional;
import java.util.UUID;

public interface LoanApplicationRepository {
    LoanApplication save(LoanApplication loanApplication);
    Optional<LoanApplication> findById(UUID id);
}
