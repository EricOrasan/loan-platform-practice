package com.btproject.loanplatform.audit_service.infrastructure.persistence;

import com.btproject.loanplatform.audit_service.domain.AuditEvent;
import org.springframework.stereotype.Component;

@Component
public class AuditEventPersistenceMapper {

    public AuditEventJpaEntity toJpaEntity(AuditEvent auditEvent) {
        return new AuditEventJpaEntity(
                auditEvent.getId(),
                auditEvent.getEventId(),
                auditEvent.getEventType(),
                auditEvent.getAggregateId(),
                auditEvent.getPayload(),
                auditEvent.getCreatedAt()
        );
    }

    public AuditEvent toDomain(AuditEventJpaEntity entity) {
        return AuditEvent.restore(
                entity.getId(),
                entity.getEventId(),
                entity.getEventType(),
                entity.getAggregateId(),
                entity.getPayload(),
                entity.getCreatedAt()
        );
    }
}
