package com.tourpackage.api.dto.response;

import java.util.UUID;

import com.tourpackage.api.entity.AmenityCategory;

public record AmenityResponse(
        UUID id,
        String name,
        String slug,
        String icon,
        AmenityCategory category,
        int displayOrder,
        boolean isActive
) {
}
