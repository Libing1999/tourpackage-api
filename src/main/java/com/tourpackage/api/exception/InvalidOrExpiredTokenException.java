package com.tourpackage.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Covers refresh, password-reset, and email-verification tokens alike —
 * all three share the same "not found / expired / already used" failure
 * mode and the same 400 response.
 */
public class InvalidOrExpiredTokenException extends ApiException {

    public InvalidOrExpiredTokenException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }

}
