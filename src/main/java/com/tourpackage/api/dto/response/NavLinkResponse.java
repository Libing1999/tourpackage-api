package com.tourpackage.api.dto.response;

import java.util.UUID;

import com.tourpackage.api.entity.NavGroup;

public record NavLinkResponse(
        UUID id,
        NavGroup navGroup,
        String label,
        String href,
        int displayOrder,
        boolean isActive
) {
}
