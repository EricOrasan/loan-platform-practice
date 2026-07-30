package com.btproject.loanplatform.loan_application_service.infrastructure.web.error;

import java.time.Instant;
import java.util.UUID;

public record ApiErrorResponse(
        String code,
        String message,
        String details,
        UUID correlationId,
        Instant timestamp
) {
}
