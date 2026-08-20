package com.btproject.loanplatform.notification_service.application.port.out;

import com.btproject.loanplatform.notification_service.domain.Notification;

import java.util.UUID;

public interface NotificationRepository {

    boolean existsByApplicationId(UUID applicationId);
    Notification save(Notification notification);
}
