package com.btproject.loanplatform.notification_service.infrastructure.notification;

import com.btproject.loanplatform.notification_service.application.port.out.NotificationSenderPort;
import com.btproject.loanplatform.notification_service.domain.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingNotificationSenderAdapter implements NotificationSenderPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingNotificationSenderAdapter.class);

    @Override
    public void send(Notification notification) {
        LOGGER.info(
                "Sending simulated {} notification for applicationId={}: {}",
                notification.getChannel(),
                notification.getApplicationId(),
                notification.getMessage()
        );
    }
}
