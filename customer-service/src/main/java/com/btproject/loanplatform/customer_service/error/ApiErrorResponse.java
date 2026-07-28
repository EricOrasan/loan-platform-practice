package com.btproject.loanplatform.customer_service.error;

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
