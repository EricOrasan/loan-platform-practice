package com.btproject.loanplatform.offer_service.application.service;

import com.btproject.loanplatform.offer_service.application.command.GenerateLoanOfferCommand;
import com.btproject.loanplatform.offer_service.application.port.in.GenerateLoanOfferUseCase;
import com.btproject.loanplatform.offer_service.application.port.out.LoanOfferEventPublisher;
import com.btproject.loanplatform.offer_service.application.port.out.LoanOfferRepository;
import com.btproject.loanplatform.offer_service.domain.AssessmentDecision;
import com.btproject.loanplatform.offer_service.domain.LoanOffer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GenerateLoanOfferService implements GenerateLoanOfferUseCase {

    private final LoanOfferRepository repository;
    private final LoanOfferEventPublisher eventPublisher;

    public GenerateLoanOfferService(LoanOfferRepository repository, LoanOfferEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void generate(GenerateLoanOfferCommand command) {
        if (command.decision() != AssessmentDecision.APPROVED) {
            return;
        }

        if (repository.existsByApplicationId(command.applicationId())) {
            return;
        }

        LoanOffer loanOffer = LoanOffer.generate(
                command.applicationId(),
                command.requestedAmount(),
                command.requestedPeriodMonths(),
                command.score()
        );

        LoanOffer savedLoanOffer = repository.save(loanOffer);

        eventPublisher.publishGenerated(savedLoanOffer);
    }
}
