package com.btproject.loanplatform.credit_assessment_service.application.command;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ProcessLoanApplicationCommand(
        UUID eventId,
        UUID applicationId,
        String cif,
        BigDecimal requestedAmount,
        int requestedPeriodMonths,
        Instant applicationCreatedAt
) {
    public ProcessLoanApplicationCommand {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(applicationId, "applicationId must not be null");
        Objects.requireNonNull(requestedAmount, "requestedAmount must not be null");
        Objects.requireNonNull(applicationCreatedAt, "applicationCreatedAt must not be null");

        if (cif == null || !cif.matches("[0-9]{8}")) {
            throw new IllegalArgumentException("CIF must contain exactly 8 digits");
        }

        if (requestedAmount.signum() <= 0) {
            throw new IllegalArgumentException("requestedAmount must be greater than 0");
        }

        if (requestedPeriodMonths < 6 || requestedPeriodMonths > 120) {
            throw new IllegalArgumentException("requestedPeriodMonths must be between 6 and 120");
        }
    }
}