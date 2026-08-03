package com.tourpackage.api.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.tourpackage.api.entity.ContentStatus;

public record BlogPostAdminResponse(
        UUID id,
        String title,
        String slug,
        String excerpt,
        String content,
        String coverImageUrl,
        String category,
        ContentStatus status,
        Instant publishedAt,
        Short readTimeMinutes,
        String authorName,
        Instant createdAt
) {
}
