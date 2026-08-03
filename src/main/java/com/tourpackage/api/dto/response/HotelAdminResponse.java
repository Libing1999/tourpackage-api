package com.tourpackage.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.tourpackage.api.entity.ContentStatus;

public record HotelAdminResponse(
        UUID id,
        String name,
        String slug,
        String description,
        String shortDescription,
        Short starRating,
        UUID cityId,
        String cityName,
        String countryName,
        String addressLine1,
        String addressLine2,
        String postalCode,
        BigDecimal latitude,
        BigDecimal longitude,
        String contactEmail,
        String contactPhone,
        String websiteUrl,
        LocalTime checkInTime,
        LocalTime checkOutTime,
        BigDecimal basePrice,
        String currencyCode,
        BigDecimal ratingAverage,
        int ratingCount,
        boolean isFeatured,
        ContentStatus status,
        String metaTitle,
        String metaDescription,
        Instant createdAt,
        Instant updatedAt,
        List<HotelImageResponse> images,
        List<AmenityResponse> amenities,
        List<HotelRoomResponse> rooms
) {
}
