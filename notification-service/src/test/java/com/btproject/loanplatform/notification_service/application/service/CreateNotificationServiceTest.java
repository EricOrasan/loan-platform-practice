package com.btproject.loanplatform.notification_service.application.service;

import com.btproject.loanplatform.notification_service.application.command.CreateNotificationCommand;
import com.btproject.loanplatform.notification_service.application.exception.CustomerContactNotFoundException;
import com.btproject.loanplatform.notification_service.application.exception.CustomerContactUnavailableException;
import com.btproject.loanplatform.notification_service.application.model.CustomerContact;
import com.btproject.loanplatform.notification_service.application.port.out.CustomerContactPort;
import com.btproject.loanplatform.notification_service.application.port.out.NotificationRepository;
import com.btproject.loanplatform.notification_service.application.port.out.NotificationSenderPort;
import com.btproject.loanplatform.notification_service.domain.Notification;
import com.btproject.loanplatform.notification_service.domain.NotificationChannel;
import com.btproject.loanplatform.notification_service.domain.NotificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateNotificationServiceTest {

    private static final UUID APPLICATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String CIF = "12345678";
    private static final String EMAIL = "andrei.popescu@example.com";

    @Mock
    private NotificationRepository repository;

    @Mock
    private CustomerContactPort customerContactPort;

    @Mock
    private NotificationSenderPort notificationSender;

    private CreateNotificationService service;

    @BeforeEach
    void setUp() {
        service = new CreateNotificationService(repository, customerContactPort, notificationSender);
    }

    @Test
    void shouldSkipApplicationThatAlreadyHasNotification() {
        when(repository.existsByApplicationId(APPLICATION_ID)).thenReturn(true);

        service.create(command());

        verify(repository, never()).save(any());
        verifyNoInteractions(customerContactPort, notificationSender);
    }

    @Test
    void shouldSendAndSaveNotificationWhenCustomerContactExists() {
        when(repository.existsByApplicationId(APPLICATION_ID)).thenReturn(false);
        when(customerContactPort.findByCif(CIF)).thenReturn(Optional.of(new CustomerContact(EMAIL)));

        service.create(command());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        Notification savedNotification = captor.getValue();

        assertEquals(APPLICATION_ID, savedNotification.getApplicationId());
        assertEquals(NotificationChannel.EMAIL, savedNotification.getChannel());
        assertEquals(EMAIL, savedNotification.getRecipient());
        assertEquals(NotificationStatus.SENT, savedNotification.getStatus());
        verify(notificationSender).send(same(savedNotification));
    }

    @Test
    void shouldNotCreateNotificationWhenCustomerContactDoesNotExist() {
        when(repository.existsByApplicationId(APPLICATION_ID)).thenReturn(false);
        when(customerContactPort.findByCif(CIF)).thenReturn(Optional.empty());

        CustomerContactNotFoundException exception = assertThrows(
                CustomerContactNotFoundException.class,
                () -> service.create(command())
        );

        assertEquals("Customer contact was not found for CIF " + CIF, exception.getMessage());
        verifyNoInteractions(notificationSender);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldPropagateCustomerContactUnavailableFailure() {
        CustomerContactUnavailableException failure = new CustomerContactUnavailableException(
                "Customer Service unavailable",
                new RuntimeException()
        );
        when(repository.existsByApplicationId(APPLICATION_ID)).thenReturn(false);
        when(customerContactPort.findByCif(CIF)).thenThrow(failure);

        CustomerContactUnavailableException thrown = assertThrows(
                CustomerContactUnavailableException.class,
                () -> service.create(command())
        );

        assertSame(failure, thrown);
        verifyNoInteractions(notificationSender);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldSaveFailedNotificationWhenSendingFails() {
        when(repository.existsByApplicationId(APPLICATION_ID)).thenReturn(false);
        when(customerContactPort.findByCif(CIF)).thenReturn(Optional.of(new CustomerContact(EMAIL)));
        doThrow(new IllegalStateException("Email provider unavailable"))
                .when(notificationSender)
                .send(any(Notification.class));

        service.create(command());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        Notification savedNotification = captor.getValue();

        assertEquals(NotificationStatus.FAILED, savedNotification.getStatus());
        assertEquals(EMAIL, savedNotification.getRecipient());
        verify(notificationSender).send(same(savedNotification));
    }

    private static CreateNotificationCommand command() {
        return new CreateNotificationCommand(APPLICATION_ID, CIF);
    }
}
