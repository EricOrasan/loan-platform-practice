package com.btproject.loanplatform.credit_assessment_service.infrastructure.messaging.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LoanApplicationCreatedEvent(
        UUID eventId,
        String eventType,
        UUID applicationId,
        String cif,
        BigDecimal requestedAmount,
        Integer requestedPeriodMonths,
        Instant createdAt
) {
}
