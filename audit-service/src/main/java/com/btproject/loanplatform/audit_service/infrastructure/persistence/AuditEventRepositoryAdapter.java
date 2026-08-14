package com.btproject.loanplatform.audit_service.infrastructure.persistence;

import com.btproject.loanplatform.audit_service.application.port.out.AuditEventRepository;
import com.btproject.loanplatform.audit_service.domain.AuditEvent;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class AuditEventRepositoryAdapter implements AuditEventRepository {

    private final SpringDataAuditEventRepository repository;
    private final AuditEventPersistenceMapper mapper;

    public AuditEventRepositoryAdapter(SpringDataAuditEventRepository repository, AuditEventPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public boolean existsByEventId(UUID eventId) {
        return repository.existsByEventId(eventId);
    }

    @Override
    public AuditEvent save(AuditEvent auditEvent) {
        AuditEventJpaEntity entity = mapper.toJpaEntity(auditEvent);
        AuditEventJpaEntity savedEntity = repository.save(entity);

        return mapper.toDomain(savedEntity);
    }
}
