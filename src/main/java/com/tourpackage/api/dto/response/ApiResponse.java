package com.tourpackage.api.dto.response;

import java.time.Instant;

/**
 * Uniform envelope for every successful response. Errors go through
 * {@link com.tourpackage.api.exception.ErrorResponse} via
 * {@link com.tourpackage.api.exception.GlobalExceptionHandler} instead —
 * successes and failures are shaped differently on purpose, so a client can
 * always tell which it got from the envelope alone, before even checking
 * the status code.
 */
public record ApiResponse<T>(boolean success, String message, T data, Instant timestamp) {

    public static <T> ApiResponse<T> of(String message, T data) {
        return new ApiResponse<>(true, message, data, Instant.now());
    }

    public static <T> ApiResponse<T> of(T data) {
        return of("Success", data);
    }

    public static ApiResponse<Void> message(String message) {
        return new ApiResponse<>(true, message, null, Instant.now());
    }

}
