package com.btproject.loanplatform.loan_application_service.infrastructure.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateLoanApplicationRequest(

        @NotBlank
        @Pattern(regexp = "^[0-9]{8}$")
        String cif,

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
