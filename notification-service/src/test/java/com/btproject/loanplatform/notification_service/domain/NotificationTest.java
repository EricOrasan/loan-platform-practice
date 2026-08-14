package com.btproject.loanplatform.notification_service.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationTest {

    private static final UUID APPLICATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String RECIPIENT = "andrei.popescu@example.com";

    @Test
    void shouldCreateEmailNotification() {
        Notification notification = Notification.create(
                APPLICATION_ID,
                NotificationChannel.EMAIL,
                RECIPIENT
        );

        assertEquals(APPLICATION_ID, notification.getApplicationId());
        assertEquals(NotificationChannel.EMAIL, notification.getChannel());
        assertEquals(RECIPIENT, notification.getRecipient());
        assertEquals(
                "Your loan offer was generated successfully for application " + APPLICATION_ID + ".",
                notification.getMessage()
        );
        assertEquals(NotificationStatus.CREATED, notification.getStatus());
    }

    @Test
    void shouldMarkNotificationAsSent() {
        Notification notification = notification();

        notification.markAsSent();

        assertEquals(NotificationStatus.SENT, notification.getStatus());
    }

    @Test
    void shouldMarkNotificationAsFailed() {
        Notification notification = notification();

        notification.markAsFailed();

        assertEquals(NotificationStatus.FAILED, notification.getStatus());
    }

    @Test
    void shouldRejectBlankRecipient() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Notification.create(APPLICATION_ID, NotificationChannel.EMAIL, "  ")
        );

        assertEquals("recipient must not be blank", exception.getMessage());
    }

    private static Notification notification() {
        return Notification.create(APPLICATION_ID, NotificationChannel.EMAIL, RECIPIENT);
    }
}
