package com.btproject.loanplatform.notification_service.infrastructure.messaging.in.exception;

public class InvalidEventPayloadException extends RuntimeException {

    public InvalidEventPayloadException(String message) {
        super(message);
    }

    public InvalidEventPayloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
