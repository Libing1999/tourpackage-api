package com.tourpackage.api.dto.response;

import java.util.UUID;

public record BannerResponse(
        UUID id,
        String title,
        String subtitle,
        String imageUrl,
        String linkUrl,
        String buttonLabel
) {
}
