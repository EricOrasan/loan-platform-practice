package com.btproject.loanplatform.credit_assessment_service.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record CustomerFinancialProfile(
        String cif,
        BigDecimal monthlyIncome,
        RiskCategory riskCategory
) {
    public CustomerFinancialProfile {

        Objects.requireNonNull(monthlyIncome, "monthlyIncome must not be null");
        Objects.requireNonNull(riskCategory, "riskCategory must not be null");

        if (cif == null || !cif.matches("[0-9]{8}")) {
            throw new IllegalArgumentException("CIF must contain exactly 8 digits");
        }

        if (monthlyIncome.signum() < 0) {
            throw new IllegalArgumentException("monthlyIncome must not be negative");
        }
    }
}
