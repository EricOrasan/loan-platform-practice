package com.btproject.loanplatform.notification_service.application.command;

import java.util.Objects;
import java.util.UUID;

public record CreateNotificationCommand(UUID applicationId, String cif) {

    public CreateNotificationCommand {
        Objects.requireNonNull(applicationId, "applicationId must not be null");

        if (cif == null || !cif.matches("[0-9]{8}")) {
            throw new IllegalArgumentException("cif must contain exactly 8 digits");
        }
    }
}
