package com.btproject.loanplatform.loan_application_service.infrastructure.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateLoanApplicationRequest(

        @NotNull(message = "Requested Amount is required")
        @DecimalMin(message = "Requested Amount must be greater than 0", value = "0", inclusive = false)
        @Digits(
                integer = 13,
                fraction = 2,
                message = "Requested Amount must have at most 13 integer digits and 2 decimal places"
        )
        BigDecimal requestedAmount,

        @NotNull(message = "Requested Period is required")
        @Min(value = 6, message = "Requested Period must be between 6 and 120 months.")
        @Max(value = 120, message = "Requested Period must be between 6 and 120 months.")
        Integer requestedPeriodMonths,

        @NotBlank(message = "Purpose is required")
        @Size(max = 255)
        String purpose
) {
}
