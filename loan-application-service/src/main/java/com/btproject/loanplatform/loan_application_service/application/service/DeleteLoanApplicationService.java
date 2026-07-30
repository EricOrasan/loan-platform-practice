package com.btproject.loanplatform.loan_application_service.application.service;

import com.btproject.loanplatform.loan_application_service.application.exception.LoanApplicationNotFoundException;
import com.btproject.loanplatform.loan_application_service.application.port.in.DeleteLoanApplicationUseCase;
import com.btproject.loanplatform.loan_application_service.application.port.out.LoanApplicationRepository;
import com.btproject.loanplatform.loan_application_service.domain.LoanApplication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DeleteLoanApplicationService implements DeleteLoanApplicationUseCase {

    private final LoanApplicationRepository repository;

    public DeleteLoanApplicationService(LoanApplicationRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        LoanApplication loanApplication = repository.findById(id)
                .orElseThrow(() -> new LoanApplicationNotFoundException(id));
        loanApplication.markAsDeleted();
        repository.save(loanApplication);
    }
}
