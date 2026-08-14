package com.btproject.loanplatform.notification_service.application.exception;

public class CustomerContactNotFoundException extends RuntimeException {

    public CustomerContactNotFoundException(String cif) {
        super("Customer contact was not found for CIF " + cif);
    }
}
