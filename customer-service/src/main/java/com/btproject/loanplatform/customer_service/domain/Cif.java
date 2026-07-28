package com.btproject.loanplatform.customer_service.domain;

import com.btproject.loanplatform.customer_service.error.InvalidCifException;

import java.util.regex.Pattern;

public record Cif(String value) {

    public static final String FORMAT_REGEX = "^[0-9]{8}$";
    public static final String VALIDATION_MESSAGE = "CIF must contain exactly 8 digits";
    private static final Pattern FORMAT = Pattern.compile(FORMAT_REGEX);

    public Cif {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new InvalidCifException();
        }
    }

    public static Cif of(String value) {
        return new Cif(value);
    }
}