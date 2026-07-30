package com.btproject.loanplatform.loan_application_service.application.service;

import com.btproject.loanplatform.loan_application_service.application.exception.LoanApplicationNotFoundException;
import com.btproject.loanplatform.loan_application_service.application.port.in.GetLoanApplicationUseCase;
import com.btproject.loanplatform.loan_application_service.application.port.out.LoanApplicationRepository;
import com.btproject.loanplatform.loan_application_service.domain.LoanApplication;
import com.btproject.loanplatform.loan_application_service.domain.LoanApplicationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetLoanApplicationService implements GetLoanApplicationUseCase {

    private final LoanApplicationRepository repository;

    public GetLoanApplicationService(LoanApplicationRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public LoanApplication get(UUID id) {
        LoanApplication loanApplication = repository.findById(id)
                .orElseThrow(() -> new LoanApplicationNotFoundException(id));

        if (loanApplication.getStatus() == LoanApplicationStatus.DELETED) {
            throw new LoanApplicationNotFoundException(loanApplication.getId());
        }

        return loanApplication;
    }
}
