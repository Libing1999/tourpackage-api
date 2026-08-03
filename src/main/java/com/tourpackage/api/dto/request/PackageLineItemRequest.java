package com.tourpackage.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Shared by both the "what's included" and "what's excluded" endpoints —
 * {@code package_includes} and {@code package_excludes} are column-for-column
 * identical, and which list a row belongs to is decided by the endpoint it
 * was posted to, not by anything in the body.
 */
public record PackageLineItemRequest(

        @NotBlank(message = "Description is required")
        @Size(max = 300, message = "Description must be at most 300 characters")
        String description,

        @Size(max = 80, message = "Icon must be at most 80 characters")
        String icon,

        int displayOrder

) {
}
