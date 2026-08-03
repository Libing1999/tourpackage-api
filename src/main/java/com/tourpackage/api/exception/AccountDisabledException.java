package com.tourpackage.api.exception;

import org.springframework.http.HttpStatus;

public class AccountDisabledException extends ApiException {

    public AccountDisabledException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }

}
