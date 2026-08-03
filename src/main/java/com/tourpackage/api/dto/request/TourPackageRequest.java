package com.tourpackage.api.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import com.tourpackage.api.entity.ContentStatus;
import com.tourpackage.api.entity.DifficultyLevel;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record TourPackageRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @NotBlank(message = "Slug is required")
        @Size(max = 220, message = "Slug must be at most 220 characters")
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "Slug must be lowercase-kebab-case")
        String slug,

        @Size(max = 500, message = "Summary must be at most 500 characters")
        String summary,

        String description,

        @NotNull(message = "Country is required")
        UUID countryId,

        @NotNull(message = "City is required")
        UUID cityId,

        @Positive(message = "Duration days must be greater than 0")
        short durationDays,

        @PositiveOrZero(message = "Duration nights cannot be negative")
        short durationNights,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", message = "Price cannot be negative")
        BigDecimal price,

        @DecimalMin(value = "0.0", message = "Discount price cannot be negative")
        BigDecimal discountPrice,

        @NotBlank(message = "Currency code is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be 3 uppercase letters")
        String currencyCode,

        @Positive(message = "Minimum group size must be greater than 0")
        short minGroupSize,

        @Positive(message = "Maximum group size must be greater than 0")
        Short maxGroupSize,

        @NotNull(message = "Difficulty level is required")
        DifficultyLevel difficultyLevel,

        boolean isFeatured,

        @NotNull(message = "Status is required")
        ContentStatus status,

        @Size(max = 255, message = "Meta title must be at most 255 characters")
        String metaTitle,

        @Size(max = 500, message = "Meta description must be at most 500 characters")
        String metaDescription

) {
}
