package com.tourpackage.api.dto.response;

import java.util.UUID;

public record PackageItineraryResponse(
        UUID id,
        short dayNumber,
        String title,
        String description,
        UUID cityId,
        String cityName,
        String meals,
        String accommodation
) {
}
