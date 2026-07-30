package com.btproject.loanplatform.loan_application_service.application.port.in;

import java.util.UUID;

public interface DeleteLoanApplicationUseCase {
    void delete(UUID id);
}
