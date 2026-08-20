package com.btproject.loanplatform.customer_service.dto;

import com.btproject.loanplatform.customer_service.domain.RiskCategory;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateCustomerRequest(

        @NotBlank(message = "CIF is required")
        @Pattern(regexp = "^[0-9]{8}$", message = "CIF must contain exactly 8 digits")
        String cif,

        @NotBlank(message = "First Name is required")
        @Size(max = 100, message = "First Name must have at most 100 characters")
        String firstName,

        @NotBlank(message = "Last Name is required")
        @Size(max = 100, message = "Last Name must have at most 100 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        @Size(max = 255, message = "Email must have at most 255 characters")
        String email,

        @NotNull(message = "Monthly income is required")
        @Positive(message = "Monthly income must be greater than zero")
        @Digits(integer = 13, fraction = 2, message = "Monthly income must have at most 13 integer digits and 2 decimal places")
        BigDecimal monthlyIncome,

        @NotNull(message = "Risk Category is required")
        RiskCategory riskCategory
) {
}
