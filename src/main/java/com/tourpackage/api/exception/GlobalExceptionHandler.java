package com.tourpackage.api.exception;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.fasterxml.jackson.databind.JsonMappingException.Reference;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, WebRequest request) {
        ErrorResponse body = ErrorResponse.of(
                ex.getStatus().value(),
                ex.getStatus().getReasonPhrase(),
                ex.getMessage(),
                path(request));
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed",
                path(request),
                fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * A body Jackson couldn't read at all: malformed JSON, or a value that
     * doesn't fit its target type — most often an enum field sent with a value
     * outside the permitted set. That's a client mistake, so it's a 400; before
     * this handler existed it fell through to the catch-all and surfaced as a
     * 500, which told the caller nothing and looked like a server fault.
     *
     * <p>Only the offending field name is echoed back, never Jackson's message
     * — that spells out the Java type and the accepted enum constants, which is
     * internal shape the public API shouldn't leak.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException ex, WebRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        String message = "Malformed request body";

        if (ex.getCause() instanceof InvalidFormatException invalidFormat) {
            String field = invalidFormat.getPath().stream()
                    .map(Reference::getFieldName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("."));

            if (!field.isBlank()) {
                Class<?> targetType = invalidFormat.getTargetType();
                fieldErrors.put(field, targetType.isEnum()
                        ? "Must be one of: " + Arrays.stream(targetType.getEnumConstants())
                                .map(String::valueOf)
                                .collect(Collectors.joining(", "))
                        : "Invalid value");
                message = "Validation failed";
            }
        }

        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                path(request),
                fieldErrors.isEmpty() ? null : fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                ex.getMessage(),
                path(request));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, WebRequest request) {
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                ex.getMessage(),
                path(request));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }


    /**
     * Wrong HTTP verb for an existing path.
     *
     * <p>405 rather than the 500 this produced before. The distinction matters
     * beyond correctness: a client error reported as a server error puts a
     * misrouted request into the same alerting bucket as a real fault.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, WebRequest request) {
        String allowed = ex.getSupportedHttpMethods() == null ? ""
                : " Allowed: " + ex.getSupportedHttpMethods().stream().map(Object::toString)
                        .collect(Collectors.joining(", "));
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase(),
                ex.getMethod() + " is not supported on this endpoint." + allowed,
                path(request));
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    /** A body this endpoint cannot read — most often a missing JSON content type. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, WebRequest request) {
        String supported = ex.getSupportedMediaTypes().isEmpty() ? ""
                : " Expected: " + ex.getSupportedMediaTypes().stream().map(Object::toString)
                        .collect(Collectors.joining(", "));
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase(),
                "Content type '" + ex.getContentType() + "' is not supported." + supported,
                path(request));
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(body);
    }

    /**
     * A query or path parameter of the wrong type — {@code ?page=abc}, a
     * malformed UUID.
     *
     * <p>Reported as a field error so it lands in the client's form-handling
     * path alongside body validation failures, rather than as a bare message.
     * The offending value is not echoed back: it is attacker-controlled and this
     * response is rendered by browsers.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        String expected = ex.getRequiredType() == null ? "the expected type"
                : ex.getRequiredType().getSimpleName();
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Invalid value for '" + ex.getName() + "'.",
                path(request),
                Map.of(ex.getName(), "Must be a valid " + expected));
        return ResponseEntity.badRequest().body(body);
    }

    /** A required query parameter was left out. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex, WebRequest request) {
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Required parameter '" + ex.getParameterName() + "' is missing.",
                path(request),
                Map.of(ex.getParameterName(), "This parameter is required"));
        return ResponseEntity.badRequest().body(body);
    }

    /** Bean Validation on a request parameter rather than a request body. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String field = violation.getPropertyPath().toString();
            // The path is "method.parameter"; only the parameter is meaningful
            // to a caller, who never saw the method name.
            fieldErrors.put(field.substring(field.lastIndexOf('.') + 1), violation.getMessage());
        });

        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed",
                path(request),
                fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * An upload past the servlet limit.
     *
     * <p>413 rather than 500, and phrased in megabytes because the caller is a
     * person choosing a file. Note this fires before any application-level size
     * check, since the container rejects the request while reading it.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadTooLarge(
            MaxUploadSizeExceededException ex, WebRequest request) {
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                HttpStatus.PAYLOAD_TOO_LARGE.getReasonPhrase(),
                "That file is too large to upload.",
                path(request));
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
    }

    /**
     * An unmapped URL.
     *
     * <p>Without this, a typo'd path falls through to the catch-all below and
     * comes back as a 500 with a stack trace in the log — so every probe, stale
     * bookmark, and client typo reads as a server fault, both to the caller and
     * to anything watching the error rate.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex, WebRequest request) {
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                "No endpoint exists at this path",
                path(request));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /**
     * Anything not handled above.
     *
     * <p>The client is told nothing beyond "something broke" — a stack trace or
     * a raw JDBC message is an information leak. But it must be logged with the
     * trace, otherwise a 500 leaves no evidence anywhere and the only way to
     * diagnose it is to reproduce it locally.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unhandled exception at {}", path(request), ex);
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An unexpected error occurred",
                path(request));
        return ResponseEntity.internalServerError().body(body);
    }

    private String path(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

}
