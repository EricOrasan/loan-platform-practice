package com.btproject.loanplatform.customer_service.error;

public class InvalidCifException extends RuntimeException {

    public InvalidCifException() {
        super("Invalid CIF format. CIF must contain exactly 8 digits.");
    }
}