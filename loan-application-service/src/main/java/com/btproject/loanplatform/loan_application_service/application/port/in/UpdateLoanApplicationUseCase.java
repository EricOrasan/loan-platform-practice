package com.btproject.loanplatform.loan_application_service.application.port.in;

import com.btproject.loanplatform.loan_application_service.application.command.UpdateLoanApplicationCommand;
import com.btproject.loanplatform.loan_application_service.domain.LoanApplication;

import java.util.UUID;

public interface UpdateLoanApplicationUseCase {
    LoanApplication update(UUID id, UpdateLoanApplicationCommand command);
}
