package com.btproject.loanplatform.credit_assessment_service.application.exception;

public class CustomerInformationUnavailableException extends RuntimeException {

    public CustomerInformationUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}