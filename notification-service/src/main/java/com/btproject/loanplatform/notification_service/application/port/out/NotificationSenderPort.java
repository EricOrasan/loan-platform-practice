package com.btproject.loanplatform.notification_service.application.port.out;

import com.btproject.loanplatform.notification_service.domain.Notification;

public interface NotificationSenderPort {

    void send(Notification notification);
}
