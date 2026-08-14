package com.btproject.loanplatform.notification_service.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataNotificationRepository extends JpaRepository<NotificationJpaEntity, UUID> {

    boolean existsByApplicationId(UUID applicationId);
}
