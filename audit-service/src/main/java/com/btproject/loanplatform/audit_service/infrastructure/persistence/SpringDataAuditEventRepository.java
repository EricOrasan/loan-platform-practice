package com.btproject.loanplatform.audit_service.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataAuditEventRepository extends JpaRepository<AuditEventJpaEntity, UUID> {

    boolean existsByEventId(UUID eventId);
}
