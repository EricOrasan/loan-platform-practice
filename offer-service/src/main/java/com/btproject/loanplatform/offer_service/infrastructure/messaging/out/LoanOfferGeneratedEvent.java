package com.btproject.loanplatform.offer_service.infrastructure.messaging.out;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LoanOfferGeneratedEvent(
        UUID eventId,
        String eventType,
        UUID applicationId,
        BigDecimal amount,
        int periodMonths,
        BigDecimal interestRate,
        BigDecimal monthlyInstallment,
        Instant createdAt
) {
}
