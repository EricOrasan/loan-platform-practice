package com.btproject.loanplatform.notification_service.application.model;

public record CustomerContact(String email) {

    public CustomerContact {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
    }
}
