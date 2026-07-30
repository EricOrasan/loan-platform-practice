package com.btproject.loanplatform.loan_application_service.application.command;

import java.math.BigDecimal;

public record UpdateLoanApplicationCommand(BigDecimal requestedAmount, int requestedPeriodMonths, String purpose) {
}
