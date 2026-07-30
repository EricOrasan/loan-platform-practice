package com.btproject.loanplatform.loan_application_service.application.service;

import com.btproject.loanplatform.loan_application_service.application.command.UpdateLoanApplicationCommand;
import com.btproject.loanplatform.loan_application_service.application.exception.LoanApplicationNotFoundException;
import com.btproject.loanplatform.loan_application_service.application.port.in.UpdateLoanApplicationUseCase;
import com.btproject.loanplatform.loan_application_service.application.port.out.LoanApplicationRepository;
import com.btproject.loanplatform.loan_application_service.domain.LoanApplication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UpdateLoanApplicationService implements UpdateLoanApplicationUseCase {

    private final LoanApplicationRepository repository;

    public UpdateLoanApplicationService(LoanApplicationRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public LoanApplication update(UUID id, UpdateLoanApplicationCommand command) {
        LoanApplication loanApplication = repository.findById(id)
                .orElseThrow(() -> new LoanApplicationNotFoundException(id));

        loanApplication.update(command.requestedAmount(), command.requestedPeriodMonths(), command.purpose());
        return repository.save(loanApplication);
    }
}
