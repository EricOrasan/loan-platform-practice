package com.btproject.loanplatform.notification_service.application.port.in;

import com.btproject.loanplatform.notification_service.application.command.CreateNotificationCommand;

public interface CreateNotificationUseCase {

    void create(CreateNotificationCommand command);
}
