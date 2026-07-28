package com.btproject.loanplatform.customer_service.error;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(String cif) {
        super("No customer exists for CIF " + cif + ".");
    }
}