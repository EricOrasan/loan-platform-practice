package com.btproject.loanplatform.offer_service.infrastructure.messaging.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LoanAssessmentCompletedEvent(
        UUID eventId,
        String eventType,
        UUID applicationId,
        String cif,
        BigDecimal requestedAmount,
        Integer requestedPeriodMonths,
        Integer score,
        String decision,
        String reason,
        Instant createdAt
) {
}
