package com.tourpackage.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record BlogPostDetailResponse(
        UUID id,
        String title,
        String slug,
        String excerpt,
        String content,
        String coverImageUrl,
        String category,
        Instant publishedAt,
        Short readTimeMinutes,
        String authorName
) {
}
