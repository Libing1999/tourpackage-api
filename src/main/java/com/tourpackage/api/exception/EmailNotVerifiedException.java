package com.tourpackage.api.exception;

import org.springframework.http.HttpStatus;

public class EmailNotVerifiedException extends ApiException {

    public EmailNotVerifiedException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }

}
