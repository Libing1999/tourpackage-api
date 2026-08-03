package com.tourpackage.api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record HotelSummaryResponse(
        UUID id,
        String name,
        String slug,
        String cityName,
        String countryName,
        String coverImageUrl,
        Short starRating,
        BigDecimal ratingAverage,
        int ratingCount,
        BigDecimal basePrice,
        String currencyCode
) {
}
