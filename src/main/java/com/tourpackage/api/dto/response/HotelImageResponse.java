package com.tourpackage.api.dto.response;

import java.util.UUID;

public record HotelImageResponse(
        UUID id,
        String url,
        String altText,
        String caption,
        int displayOrder,
        boolean isCover
) {
}
