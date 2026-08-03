package com.tourpackage.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record BlogPostSummaryResponse(
        UUID id,
        String title,
        String slug,
        String excerpt,
        String coverImageUrl,
        String category,
        Instant publishedAt,
        Short readTimeMinutes,
        String authorName
) {
}
