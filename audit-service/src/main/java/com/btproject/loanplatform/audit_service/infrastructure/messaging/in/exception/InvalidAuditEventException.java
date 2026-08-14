package com.btproject.loanplatform.audit_service.infrastructure.messaging.in.exception;

public class InvalidAuditEventException extends RuntimeException {

    public InvalidAuditEventException(String message) {
        super(message);
    }

    public InvalidAuditEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
