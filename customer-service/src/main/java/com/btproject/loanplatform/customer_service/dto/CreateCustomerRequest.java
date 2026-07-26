package com.btproject.loanplatform.customer_service.dto;

import com.btproject.loanplatform.customer_service.entity.RiskCategory;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateCustomerRequest(

        @NotBlank(message = "CIF is required")
        @Size(max = 20, message = "CIF must be at most 20 characters")
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

        @NotNull(message = "Monthly Income is required")
        @Positive(message = "Monthly Income must be greater than zero")
        BigDecimal monthlyIncome,

        @NotNull(message = "Risk Category is required")
        RiskCategory riskCategory
) {
}
