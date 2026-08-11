package com.btproject.loanplatform.offer_service.application.port.out;

import com.btproject.loanplatform.offer_service.domain.LoanOffer;

public interface LoanOfferEventPublisher {

    void publishGenerated(LoanOffer loanOffer);
}
