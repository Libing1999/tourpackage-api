package com.tourpackage.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record MediaAssetResponse(
        UUID id,
        String url,
        String thumbnailUrl,
        String originalFilename,
        String contentType,
        long sizeBytes,
        int width,
        int height,
        String folder,
        Instant createdAt
) {
}
