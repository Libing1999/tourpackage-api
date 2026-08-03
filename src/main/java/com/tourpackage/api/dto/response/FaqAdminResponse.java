package com.tourpackage.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record FaqAdminResponse(
        UUID id,
        String question,
        String answer,
        String category,
        int displayOrder,
        boolean isActive,
        Instant createdAt
) {
}
