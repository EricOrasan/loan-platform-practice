package com.btproject.loanplatform.loan_application_service.application.port.out;

import com.btproject.loanplatform.loan_application_service.domain.LoanApplication;

public interface LoanApplicationEventPublisher {
    void publishCreated(LoanApplication loanApplication);
}
