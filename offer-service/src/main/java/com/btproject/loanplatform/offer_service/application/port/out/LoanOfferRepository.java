package com.btproject.loanplatform.offer_service.application.port.out;

import com.btproject.loanplatform.offer_service.domain.LoanOffer;

import java.util.UUID;

public interface LoanOfferRepository {

    boolean existsByApplicationId(UUID applicationId);
    LoanOffer save(LoanOffer loanOffer);
}