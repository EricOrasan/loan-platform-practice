package com.btproject.loanplatform.loan_application_service.infrastructure.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateLoanApplicationRequest(

        @NotNull
        @DecimalMin(value = "0", inclusive = false)
        BigDecimal requestedAmount,

        @NotNull
        @Min(6)
        @Max(120)
        Integer requestedPeriodMonths,

        @NotBlank
        @Size(max = 255)
        String purpose
) {
}
