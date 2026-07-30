package com.btproject.loanplatform.loan_application_service.application.port.in;

import com.btproject.loanplatform.loan_application_service.domain.LoanApplication;

import java.util.UUID;

public interface GetLoanApplicationUseCase {
    LoanApplication get(UUID id);
}
