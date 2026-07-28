package com.btproject.loanplatform.customer_service.dto;

import com.btproject.loanplatform.customer_service.domain.RiskCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String cif,
        String firstName,
        String lastName,
        String email,
        BigDecimal monthlyIncome,
        RiskCategory riskCategory,
        Instant createdAt,
        Instant updatedAt
) {
}
