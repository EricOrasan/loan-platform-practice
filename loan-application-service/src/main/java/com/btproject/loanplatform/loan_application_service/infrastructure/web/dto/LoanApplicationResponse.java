package com.btproject.loanplatform.loan_application_service.infrastructure.web.dto;

import com.btproject.loanplatform.loan_application_service.domain.LoanApplicationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LoanApplicationResponse(
        UUID id,
        String applicationNumber,
        String cif,
        BigDecimal requestedAmount,
        int requestedPeriodMonths,
        String purpose,
        LoanApplicationStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
