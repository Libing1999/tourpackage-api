package com.tourpackage.api.dto.request;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.tourpackage.api.entity.ContentStatus;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record HotelRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 200, message = "Name must be at most 200 characters")
        String name,

        @NotBlank(message = "Slug is required")
        @Size(max = 220, message = "Slug must be at most 220 characters")
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "Slug must be lowercase-kebab-case")
        String slug,

        String description,

        @Size(max = 500, message = "Short description must be at most 500 characters")
        String shortDescription,

        @Min(value = 1, message = "Star rating must be between 1 and 5")
        @Max(value = 5, message = "Star rating must be between 1 and 5")
        Short starRating,

        @NotNull(message = "City is required")
        UUID cityId,

        @NotBlank(message = "Address is required")
        @Size(max = 255, message = "Address line 1 must be at most 255 characters")
        String addressLine1,

        @Size(max = 255, message = "Address line 2 must be at most 255 characters")
        String addressLine2,

        @Size(max = 20, message = "Postal code must be at most 20 characters")
        String postalCode,

        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
        BigDecimal latitude,

        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
        BigDecimal longitude,

        @Email(message = "Contact email must be a valid address")
        String contactEmail,

        @Size(max = 20, message = "Contact phone must be at most 20 characters")
        String contactPhone,

        String websiteUrl,

        LocalTime checkInTime,

        LocalTime checkOutTime,

        @NotNull(message = "Base price is required")
        @DecimalMin(value = "0.0", message = "Base price cannot be negative")
        BigDecimal basePrice,

        @NotBlank(message = "Currency code is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be 3 uppercase letters")
        String currencyCode,

        boolean isFeatured,

        @NotNull(message = "Status is required")
        ContentStatus status,

        @Size(max = 255, message = "Meta title must be at most 255 characters")
        String metaTitle,

        @Size(max = 500, message = "Meta description must be at most 500 characters")
        String metaDescription,

        List<UUID> amenityIds

) {
}
