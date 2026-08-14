package com.btproject.loanplatform.notification_service.infrastructure.messaging.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LoanOfferGeneratedEvent(
        UUID eventId,
        String eventType,
        UUID applicationId,
        String cif,
        BigDecimal amount,
        Integer periodMonths,
        BigDecimal interestRate,
        BigDecimal monthlyInstallment,
        Instant createdAt
) {
}
