package com.btproject.loanplatform.credit_assessment_service.infrastructure.customer;

import com.btproject.loanplatform.credit_assessment_service.domain.RiskCategory;

import java.math.BigDecimal;

public record CustomerServiceResponse(
        String cif,
        BigDecimal monthlyIncome,
        RiskCategory riskCategory
) {
}
