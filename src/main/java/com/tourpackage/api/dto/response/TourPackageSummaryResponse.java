package com.tourpackage.api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record TourPackageSummaryResponse(
        UUID id,
        String title,
        String slug,
        String cityName,
        String countryName,
        String coverImageUrl,
        short durationDays,
        short durationNights,
        BigDecimal price,
        BigDecimal discountPrice,
        BigDecimal ratingAverage,
        int ratingCount
) {
}
