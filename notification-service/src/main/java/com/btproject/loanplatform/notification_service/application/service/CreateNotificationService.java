package com.btproject.loanplatform.notification_service.application.service;

import com.btproject.loanplatform.notification_service.application.command.CreateNotificationCommand;
import com.btproject.loanplatform.notification_service.application.exception.CustomerContactNotFoundException;
import com.btproject.loanplatform.notification_service.application.model.CustomerContact;
import com.btproject.loanplatform.notification_service.application.port.in.CreateNotificationUseCase;
import com.btproject.loanplatform.notification_service.application.port.out.CustomerContactPort;
import com.btproject.loanplatform.notification_service.application.port.out.NotificationRepository;
import com.btproject.loanplatform.notification_service.application.port.out.NotificationSenderPort;
import com.btproject.loanplatform.notification_service.domain.Notification;
import com.btproject.loanplatform.notification_service.domain.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CreateNotificationService implements CreateNotificationUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateNotificationService.class);

    private final NotificationRepository repository;
    private final CustomerContactPort customerContactPort;
    private final NotificationSenderPort notificationSender;

    public CreateNotificationService(NotificationRepository repository, CustomerContactPort customerContactPort, NotificationSenderPort notificationSender) {
        this.repository = repository;
        this.customerContactPort = customerContactPort;
        this.notificationSender = notificationSender;
    }

    @Override
    public void create(CreateNotificationCommand command) {
        if (repository.existsByApplicationId(command.applicationId())) {
            LOGGER.info(
                    "Notification already exists for applicationId={}; skipping duplicate event",
                    command.applicationId()
            );
            return;
        }

        CustomerContact contact = customerContactPort
                .findByCif(command.cif())
                .orElseThrow(() -> new CustomerContactNotFoundException(command.cif()));

        Notification notification = Notification.create(
                command.applicationId(),
                NotificationChannel.EMAIL,
                contact.email()
        );

        try {
            notificationSender.send(notification);
            notification.markAsSent();
        } catch (RuntimeException exception) {
            notification.markAsFailed();
            LOGGER.error(
                    "Failed to send notification for applicationId={}",
                    command.applicationId(),
                    exception
            );
        }

        repository.save(notification);
    }
}
