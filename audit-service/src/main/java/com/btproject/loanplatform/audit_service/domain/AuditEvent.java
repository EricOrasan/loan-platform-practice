package com.btproject.loanplatform.audit_service.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class AuditEvent {

    private final UUID id;
    private final UUID eventId;
    private final AuditEventType eventType;
    private final UUID aggregateId;
    private final String payload;
    private final Instant createdAt;

    private AuditEvent(UUID id, UUID eventId, AuditEventType eventType, UUID aggregateId, String payload, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        this.payload = validatePayload(payload);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static AuditEvent create(UUID eventId, AuditEventType eventType, UUID aggregateId, String payload) {
        return new AuditEvent(UUID.randomUUID(), eventId, eventType, aggregateId, payload, Instant.now());
    }

    public static AuditEvent restore(UUID id, UUID eventId, AuditEventType eventType, UUID aggregateId, String payload, Instant createdAt) {
        return new AuditEvent(id, eventId, eventType, aggregateId, payload, createdAt);
    }

    private static String validatePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("payload must not be blank");
        }

        return payload;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
