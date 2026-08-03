package com.tourpackage.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.tourpackage.api.entity.ContentStatus;
import com.tourpackage.api.entity.DifficultyLevel;

public record TourPackageAdminResponse(
        UUID id,
        String title,
        String slug,
        String summary,
        String description,
        UUID countryId,
        String countryName,
        UUID cityId,
        String cityName,
        short durationDays,
        short durationNights,
        BigDecimal price,
        BigDecimal discountPrice,
        String currencyCode,
        short minGroupSize,
        Short maxGroupSize,
        DifficultyLevel difficultyLevel,
        BigDecimal ratingAverage,
        int ratingCount,
        boolean isFeatured,
        ContentStatus status,
        String metaTitle,
        String metaDescription,
        Instant createdAt,
        Instant updatedAt,
        List<PackageImageResponse> images,
        List<PackageItineraryResponse> itinerary,
        List<PackageLineItemResponse> includes,
        List<PackageLineItemResponse> excludes
) {
}
