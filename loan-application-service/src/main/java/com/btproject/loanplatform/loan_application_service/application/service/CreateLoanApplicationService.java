package com.btproject.loanplatform.loan_application_service.application.service;

import com.btproject.loanplatform.loan_application_service.application.command.CreateLoanApplicationCommand;
import com.btproject.loanplatform.loan_application_service.application.port.in.CreateLoanApplicationUseCase;
import com.btproject.loanplatform.loan_application_service.application.port.out.LoanApplicationRepository;
import com.btproject.loanplatform.loan_application_service.domain.LoanApplication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
public class CreateLoanApplicationService implements CreateLoanApplicationUseCase {

    private final LoanApplicationRepository repository;

    public CreateLoanApplicationService(LoanApplicationRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public LoanApplication create(CreateLoanApplicationCommand command) {
        LoanApplication loanApplication = new LoanApplication(command.cif(), command.requestedAmount(), command.requestedPeriodMonths(), command.purpose());
        return repository.save(loanApplication);
    }
}
