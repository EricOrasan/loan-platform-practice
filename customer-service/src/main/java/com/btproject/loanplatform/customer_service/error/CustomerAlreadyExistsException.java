package com.btproject.loanplatform.customer_service.error;

public class CustomerAlreadyExistsException extends RuntimeException {

    private CustomerAlreadyExistsException(String message) {
        super(message);
    }

    public static CustomerAlreadyExistsException forCif(String cif) {
        return new CustomerAlreadyExistsException(
                "A customer already exists for CIF " + cif + "."
        );
    }

    public static CustomerAlreadyExistsException forEmail() {
        return new CustomerAlreadyExistsException(
                "A customer already exists with the provided email address."
        );
    }
}
