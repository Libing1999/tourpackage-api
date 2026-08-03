package com.tourpackage.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Base unchecked exception for domain/application errors that should be
 * translated into a specific HTTP status by {@link GlobalExceptionHandler}.
 * Feature modules should throw this (or a subclass) instead of generic
 * RuntimeExceptions.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

}
