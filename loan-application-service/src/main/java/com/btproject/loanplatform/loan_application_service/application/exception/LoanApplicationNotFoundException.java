package com.btproject.loanplatform.loan_application_service.application.exception;

import java.util.UUID;

public class LoanApplicationNotFoundException extends RuntimeException {
    public LoanApplicationNotFoundException(UUID id) {
        super("Loan Application Not Found: " + id);
    }
}
