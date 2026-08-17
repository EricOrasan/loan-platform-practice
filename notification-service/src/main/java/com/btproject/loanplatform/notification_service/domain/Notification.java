package com.btproject.loanplatform.notification_service.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Notification {

    private final UUID id;
    private final UUID applicationId;
    private final NotificationChannel channel;
    private final String recipient;
    private final String message;
    private NotificationStatus status;
    private final Instant createdAt;

    private Notification(UUID id, UUID applicationId, NotificationChannel channel, String recipient, String message, NotificationStatus status, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.applicationId = Objects.requireNonNull(applicationId, "applicationId must not be null");
        this.channel = Objects.requireNonNull(channel, "channel must not be null");
        this.recipient = validateRecipient(recipient);
        this.message = Objects.requireNonNull(message, "message must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static Notification create(UUID applicationId, NotificationChannel channel, String recipient) {
        String message = "Your loan offer was generated successfully for application " + applicationId + ".";

        return new Notification(UUID.randomUUID(), applicationId, channel, recipient, message, NotificationStatus.CREATED, Instant.now());
    }

    public static Notification restore(UUID id, UUID applicationId, NotificationChannel channel, String recipient, String message, NotificationStatus status, Instant createdAt) {
        return new Notification(id, applicationId, channel, recipient, message, status, createdAt);
    }

    public void markAsSent() {
        status = NotificationStatus.SENT;
    }

    public void markAsFailed() {
        status = NotificationStatus.FAILED;
    }

    private static String validateRecipient(String recipient) {
        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException("recipient must not be blank");
        }

        return recipient;
    }

    public UUID getId() {
        return id;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getMessage() {
        return message;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
