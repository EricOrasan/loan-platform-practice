package com.btproject.loanplatform.audit_service.application.service;

import com.btproject.loanplatform.audit_service.application.command.RecordAuditEventCommand;
import com.btproject.loanplatform.audit_service.application.port.out.AuditEventRepository;
import com.btproject.loanplatform.audit_service.domain.AuditEvent;
import com.btproject.loanplatform.audit_service.domain.AuditEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordAuditEventServiceTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID AGGREGATE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String PAYLOAD = "{\"eventType\":\"LOAN_OFFER_GENERATED\"}";

    @Mock
    private AuditEventRepository repository;

    private RecordAuditEventService service;

    @BeforeEach
    void setUp() {
        service = new RecordAuditEventService(repository);
    }

    @Test
    void shouldSaveNewAuditEvent() {
        RecordAuditEventCommand command = command();
        when(repository.existsByEventId(EVENT_ID)).thenReturn(false);

        service.record(command);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repository).save(captor.capture());

        AuditEvent savedEvent = captor.getValue();
        assertEquals(EVENT_ID, savedEvent.getEventId());
        assertEquals(AuditEventType.LOAN_OFFER_GENERATED, savedEvent.getEventType());
        assertEquals(AGGREGATE_ID, savedEvent.getAggregateId());
        assertEquals(PAYLOAD, savedEvent.getPayload());
    }

    @Test
    void shouldSkipDuplicateAuditEvent() {
        RecordAuditEventCommand command = command();
        when(repository.existsByEventId(EVENT_ID)).thenReturn(true);

        service.record(command);

        verify(repository, never()).save(any());
    }

    private static RecordAuditEventCommand command() {
        return new RecordAuditEventCommand(
                EVENT_ID,
                AuditEventType.LOAN_OFFER_GENERATED,
                AGGREGATE_ID,
                PAYLOAD
        );
    }
}
