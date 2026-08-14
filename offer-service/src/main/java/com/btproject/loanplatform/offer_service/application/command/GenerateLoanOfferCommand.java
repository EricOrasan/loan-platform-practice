package com.btproject.loanplatform.offer_service.application.command;

import com.btproject.loanplatform.offer_service.domain.AssessmentDecision;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record GenerateLoanOfferCommand(
        UUID eventId,
        UUID applicationId,
        String cif,
        BigDecimal requestedAmount,
        int requestedPeriodMonths,
        int score,
        AssessmentDecision decision,
        Instant assessmentCreatedAt
) {
    public GenerateLoanOfferCommand {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(applicationId, "applicationId must not be null");
        Objects.requireNonNull(requestedAmount, "requestedAmount must not be null");
        Objects.requireNonNull(decision, "decision must not be null");
        Objects.requireNonNull(assessmentCreatedAt, "assessmentCreatedAt must not be null");

        if (cif == null || !cif.matches("[0-9]{8}")) {
            throw new IllegalArgumentException("cif must contain exactly 8 digits");
        }

        if (requestedAmount.signum() <= 0) {
            throw new IllegalArgumentException("requestedAmount must be greater than 0");
        }

        if (requestedPeriodMonths < 6 || requestedPeriodMonths > 120) {
            throw new IllegalArgumentException("requestedPeriodMonths must be between 6 and 120");
        }

        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("score must be between 0 and 100");
        }
    }
}
