package com.btproject.loanplatform.audit_service.application.command;

import com.btproject.loanplatform.audit_service.domain.AuditEventType;

import java.util.Objects;
import java.util.UUID;

public record RecordAuditEventCommand(
        UUID eventId,
        AuditEventType eventType,
        UUID aggregateId,
        String payload
) {

    public RecordAuditEventCommand {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");

        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("payload must not be blank");
        }
    }
}
