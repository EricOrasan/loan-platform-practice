package com.btproject.loanplatform.loan_application_service.application.port.in;

import com.btproject.loanplatform.loan_application_service.application.command.CreateLoanApplicationCommand;
import com.btproject.loanplatform.loan_application_service.domain.LoanApplication;

public interface CreateLoanApplicationUseCase {
    LoanApplication create(CreateLoanApplicationCommand command);
}
