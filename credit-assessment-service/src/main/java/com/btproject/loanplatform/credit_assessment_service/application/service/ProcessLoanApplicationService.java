package com.btproject.loanplatform.credit_assessment_service.application.service;

import com.btproject.loanplatform.credit_assessment_service.application.command.ProcessLoanApplicationCommand;
import com.btproject.loanplatform.credit_assessment_service.application.exception.CustomerInformationUnavailableException;
import com.btproject.loanplatform.credit_assessment_service.application.port.in.ProcessLoanApplicationUseCase;
import com.btproject.loanplatform.credit_assessment_service.application.port.out.CreditAssessmentEventPublisher;
import com.btproject.loanplatform.credit_assessment_service.application.port.out.CreditAssessmentRepository;
import com.btproject.loanplatform.credit_assessment_service.application.port.out.CustomerInformationPort;
import com.btproject.loanplatform.credit_assessment_service.domain.CreditAssessment;
import com.btproject.loanplatform.credit_assessment_service.domain.CustomerFinancialProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ProcessLoanApplicationService implements ProcessLoanApplicationUseCase {

    private final CreditAssessmentRepository repository;
    private final CustomerInformationPort customerInformationPort;
    private final CreditAssessmentEventPublisher eventPublisher;

    public ProcessLoanApplicationService(CreditAssessmentRepository repository, CustomerInformationPort customerInformationPort, CreditAssessmentEventPublisher eventPublisher) {
        this.repository = repository;
        this.customerInformationPort = customerInformationPort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void process(ProcessLoanApplicationCommand command) {

        if (repository.existsByApplicationId(command.applicationId())) {
            return;
        }

        CreditAssessment assessment;

        try {
            Optional<CustomerFinancialProfile> customerOptional = customerInformationPort.findByCif(command.cif());

            if (customerOptional.isPresent()) {
                CustomerFinancialProfile customer = customerOptional.get();
                assessment = CreditAssessment.assess(command.applicationId(), command.cif(), customer);
            } else {
                assessment = CreditAssessment.customerNotFound(command.applicationId(), command.cif());
            }
        } catch (CustomerInformationUnavailableException exception) {
            assessment = CreditAssessment.technicalFailure(command.applicationId(), command.cif());
        }

        CreditAssessment savedAssessment = repository.save(assessment);
        eventPublisher.publishCompleted(savedAssessment, command.requestedAmount(), command.requestedPeriodMonths());
    }
}
