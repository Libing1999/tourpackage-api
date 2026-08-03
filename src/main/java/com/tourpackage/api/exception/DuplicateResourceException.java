package com.tourpackage.api.exception;

import org.springframework.http.HttpStatus;

/** A unique constraint the caller could have avoided by checking first — a
 * slug, an amenity name, etc. Distinct from validation errors (400): the
 * request is well-formed, it just collides with something that already
 * exists. */
public class DuplicateResourceException extends ApiException {

    public DuplicateResourceException(String message) {
        super(HttpStatus.CONFLICT, message);
    }

}
