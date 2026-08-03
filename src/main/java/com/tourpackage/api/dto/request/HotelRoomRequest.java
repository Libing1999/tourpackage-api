package com.tourpackage.api.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record HotelRoomRequest(

        @NotNull(message = "Room type is required")
        java.util.UUID roomTypeId,

        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        String description,

        @Positive(message = "Max adults must be greater than 0")
        short maxAdults,

        @PositiveOrZero(message = "Max children cannot be negative")
        short maxChildren,

        @Positive(message = "Bed count must be greater than 0")
        short bedCount,

        @Size(max = 50, message = "Bed type must be at most 50 characters")
        String bedType,

        @DecimalMin(value = "0.0", inclusive = false, message = "Size must be greater than 0")
        BigDecimal sizeSqm,

        @NotNull(message = "Price per night is required")
        @DecimalMin(value = "0.0", message = "Price per night cannot be negative")
        BigDecimal pricePerNight,

        @NotBlank(message = "Currency code is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be 3 uppercase letters")
        String currencyCode,

        @Min(value = 0, message = "Total rooms cannot be negative")
        int totalRooms,

        boolean isActive

) {
}
