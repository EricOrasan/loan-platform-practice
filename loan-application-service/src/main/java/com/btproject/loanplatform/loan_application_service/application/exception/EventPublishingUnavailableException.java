package com.btproject.loanplatform.loan_application_service.application.exception;

public class EventPublishingUnavailableException extends RuntimeException {

    public EventPublishingUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}