package com.tourpackage.api.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.tourpackage.api.entity.BannerPlacement;

public record BannerAdminResponse(
        UUID id,
        BannerPlacement placement,
        String title,
        String subtitle,
        String imageUrl,
        String linkUrl,
        String buttonLabel,
        int displayOrder,
        boolean isActive,
        Instant startsAt,
        Instant endsAt
) {
}
