package com.tourpackage.api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import com.tourpackage.api.entity.ContentStatus;
import com.tourpackage.api.entity.DifficultyLevel;

/** Row-level shape for the paginated admin table — no nested collections, see
 * {@link HotelAdminListResponse} for the same rationale. */
public record TourPackageAdminListResponse(
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
        String currencyCode,
        DifficultyLevel difficultyLevel,
        BigDecimal ratingAverage,
        int ratingCount,
        boolean isFeatured,
        ContentStatus status
) {
}
