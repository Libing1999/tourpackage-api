package com.tourpackage.api.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PackageItineraryRequest(

        @Positive(message = "Day number must be greater than 0")
        short dayNumber,

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        String description,

        UUID cityId,

        @Size(max = 100, message = "Meals must be at most 100 characters")
        String meals,

        @Size(max = 200, message = "Accommodation must be at most 200 characters")
        String accommodation

) {
}
