package com.btproject.loanplatform.offer_service.infrastructure.persistence;

import com.btproject.loanplatform.offer_service.domain.LoanOffer;
import org.springframework.stereotype.Component;

@Component
public class LoanOfferPersistenceMapper {

    public LoanOfferJpaEntity toJpaEntity(LoanOffer loanOffer) {
        return new LoanOfferJpaEntity(
                loanOffer.getId(),
                loanOffer.getApplicationId(),
                loanOffer.getAmount(),
                loanOffer.getPeriodMonths(),
                loanOffer.getInterestRate(),
                loanOffer.getMonthlyInstallment(),
                loanOffer.getStatus(),
                loanOffer.getCreatedAt()
        );
    }

    public LoanOffer toDomain(LoanOfferJpaEntity entity) {
        return LoanOffer.restore(
                entity.getId(),
                entity.getApplicationId(),
                entity.getAmount(),
                entity.getPeriodMonths(),
                entity.getInterestRate(),
                entity.getMonthlyInstallment(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
