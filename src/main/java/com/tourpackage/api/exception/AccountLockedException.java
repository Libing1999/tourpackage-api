package com.tourpackage.api.exception;

import org.springframework.http.HttpStatus;

public class AccountLockedException extends ApiException {

    public AccountLockedException(String message) {
        super(HttpStatus.LOCKED, message);
    }

}
