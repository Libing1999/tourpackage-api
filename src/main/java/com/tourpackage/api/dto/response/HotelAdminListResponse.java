package com.tourpackage.api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import com.tourpackage.api.entity.ContentStatus;

/**
 * Card/row-level shape for the paginated admin hotel table. Deliberately
 * leaves out images/amenities/rooms — fetching those for every row on a
 * list page would be N+1 for no reason; {@link HotelAdminResponse} (the
 * single-hotel GET) is where the full nested detail lives.
 */
public record HotelAdminListResponse(
        UUID id,
        String name,
        String slug,
        String cityName,
        String countryName,
        String coverImageUrl,
        Short starRating,
        BigDecimal basePrice,
        String currencyCode,
        BigDecimal ratingAverage,
        int ratingCount,
        boolean isFeatured,
        ContentStatus status
) {
}
