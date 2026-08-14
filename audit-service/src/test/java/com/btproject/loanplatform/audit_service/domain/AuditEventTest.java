package com.btproject.loanplatform.audit_service.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditEventTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID AGGREGATE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String PAYLOAD = "{\"eventType\":\"LOAN_APPLICATION_CREATED\"}";

    @Test
    void shouldCreateAuditEvent() {
        AuditEvent event = AuditEvent.create(
                EVENT_ID,
                AuditEventType.LOAN_APPLICATION_CREATED,
                AGGREGATE_ID,
                PAYLOAD
        );

        assertNotNull(event.getId());
        assertEquals(EVENT_ID, event.getEventId());
        assertEquals(AuditEventType.LOAN_APPLICATION_CREATED, event.getEventType());
        assertEquals(AGGREGATE_ID, event.getAggregateId());
        assertEquals(PAYLOAD, event.getPayload());
        assertNotNull(event.getCreatedAt());
    }

    @Test
    void shouldRestoreAuditEventWithoutChangingPersistedValues() {
        UUID id = UUID.fromString("33333333-3333-3333-3333-333333333333");
        Instant createdAt = Instant.parse("2026-08-14T10:00:00Z");

        AuditEvent event = AuditEvent.restore(
                id,
                EVENT_ID,
                AuditEventType.LOAN_ASSESSMENT_COMPLETED,
                AGGREGATE_ID,
                PAYLOAD,
                createdAt
        );

        assertEquals(id, event.getId());
        assertEquals(EVENT_ID, event.getEventId());
        assertEquals(AuditEventType.LOAN_ASSESSMENT_COMPLETED, event.getEventType());
        assertEquals(AGGREGATE_ID, event.getAggregateId());
        assertEquals(PAYLOAD, event.getPayload());
        assertEquals(createdAt, event.getCreatedAt());
    }

    @Test
    void shouldRejectBlankPayload() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AuditEvent.create(
                        EVENT_ID,
                        AuditEventType.LOAN_OFFER_GENERATED,
                        AGGREGATE_ID,
                        " "
                )
        );

        assertEquals("payload must not be blank", exception.getMessage());
    }

    @Test
    void shouldRejectMissingEventId() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> AuditEvent.create(
                        null,
                        AuditEventType.LOAN_APPLICATION_CREATED,
                        AGGREGATE_ID,
                        PAYLOAD
                )
        );

        assertEquals("eventId must not be null", exception.getMessage());
    }
}
