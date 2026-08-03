package com.tourpackage.api.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.tourpackage.api.entity.DifficultyLevel;

public record TourPackagePublicDetailResponse(
        UUID id,
        String title,
        String slug,
        String summary,
        String description,
        String countryName,
        String cityName,
        short durationDays,
        short durationNights,
        BigDecimal price,
        BigDecimal discountPrice,
        /** Effective per-person rates, resolved server-side so the booking
         * form can total a party without re-deriving the pricing rules. */
        BigDecimal pricePerAdult,
        BigDecimal pricePerChild,
        String currencyCode,
        short minGroupSize,
        Short maxGroupSize,
        DifficultyLevel difficultyLevel,
        BigDecimal ratingAverage,
        int ratingCount,
        String metaTitle,
        String metaDescription,
        List<PackageImageResponse> images,
        List<PackageItineraryResponse> itinerary,
        List<PackageLineItemResponse> includes,
        List<PackageLineItemResponse> excludes
) {
}
