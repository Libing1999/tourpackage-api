package com.tourpackage.api.dto.response;

import java.util.UUID;

public record ContentBlockResponse(
        UUID id,
        String key,
        String eyebrow,
        String title,
        String subtitle,
        String body,
        boolean isActive
) {
}
