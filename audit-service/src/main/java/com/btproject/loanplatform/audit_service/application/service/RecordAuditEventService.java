package com.btproject.loanplatform.audit_service.application.service;

import com.btproject.loanplatform.audit_service.application.command.RecordAuditEventCommand;
import com.btproject.loanplatform.audit_service.application.port.in.RecordAuditEventUseCase;
import com.btproject.loanplatform.audit_service.application.port.out.AuditEventRepository;
import com.btproject.loanplatform.audit_service.domain.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecordAuditEventService implements RecordAuditEventUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecordAuditEventService.class);

    private final AuditEventRepository repository;

    public RecordAuditEventService(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void record(RecordAuditEventCommand command) {
        if (repository.existsByEventId(command.eventId())) {
            LOGGER.info(
                    "Audit event already exists for eventId={}; skipping duplicate event",
                    command.eventId()
            );
            return;
        }

        AuditEvent auditEvent = AuditEvent.create(
                command.eventId(),
                command.eventType(),
                command.aggregateId(),
                command.payload()
        );

        repository.save(auditEvent);
    }
}
