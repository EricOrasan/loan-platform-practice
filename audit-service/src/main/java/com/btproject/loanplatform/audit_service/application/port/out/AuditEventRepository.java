package com.btproject.loanplatform.audit_service.application.port.out;

import com.btproject.loanplatform.audit_service.domain.AuditEvent;

import java.util.UUID;

public interface AuditEventRepository {

    boolean existsByEventId(UUID eventId);
    AuditEvent save(AuditEvent auditEvent);
}
