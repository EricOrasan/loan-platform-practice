package com.btproject.loanplatform.offer_service.application.port.in;

import com.btproject.loanplatform.offer_service.application.command.GenerateLoanOfferCommand;

public interface GenerateLoanOfferUseCase {

    void generate(GenerateLoanOfferCommand command);
}
