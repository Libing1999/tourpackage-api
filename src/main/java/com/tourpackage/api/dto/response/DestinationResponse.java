package com.tourpackage.api.dto.response;

import java.util.UUID;

public record DestinationResponse(
        UUID id,
        String name,
        String slug,
        String countryName,
        String imageUrl,
        long packageCount
) {
}
