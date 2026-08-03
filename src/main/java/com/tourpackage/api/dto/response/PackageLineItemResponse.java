package com.tourpackage.api.dto.response;

import java.util.UUID;

/** Shared by includes and excludes — see {@code PackageLineItemRequest}. */
public record PackageLineItemResponse(
        UUID id,
        String description,
        String icon,
        int displayOrder
) {
}
