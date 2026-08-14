package com.btproject.loanplatform.notification_service.infrastructure.persistence;

import com.btproject.loanplatform.notification_service.domain.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationPersistenceMapper {

    public NotificationJpaEntity toJpaEntity(Notification notification) {
        return new NotificationJpaEntity(
                notification.getId(),
                notification.getApplicationId(),
                notification.getChannel(),
                notification.getRecipient(),
                notification.getMessage(),
                notification.getStatus(),
                notification.getCreatedAt()
        );
    }

    public Notification toDomain(NotificationJpaEntity entity) {
        return Notification.restore(
                entity.getId(),
                entity.getApplicationId(),
                entity.getChannel(),
                entity.getRecipient(),
                entity.getMessage(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
