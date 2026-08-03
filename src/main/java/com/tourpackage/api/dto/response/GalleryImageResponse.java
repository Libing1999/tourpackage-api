package com.tourpackage.api.dto.response;

import java.util.UUID;

public record GalleryImageResponse(
        UUID id,
        String url,
        String altText,
        String caption,
        String category,
        int displayOrder,
        boolean isActive
) {
}
