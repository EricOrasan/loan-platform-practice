package com.btproject.loanplatform.loan_application_service.domain.exception;

import com.btproject.loanplatform.loan_application_service.domain.LoanApplicationStatus;

public class InvalidLoanApplicationStatusException extends RuntimeException {

    public InvalidLoanApplicationStatusException(LoanApplicationStatus status, String operation) {
        super("Cannot " + operation + " loan application in status " + status);
    }
}
