package com.tourpackage.api.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TestimonialRequest(

        @NotBlank(message = "Customer name is required")
        @Size(max = 150, message = "Customer name must be at most 150 characters")
        String customerName,

        String customerAvatarUrl,

        UUID customerCountryId,

        UUID packageId,

        @Min(value = 1, message = "Rating must be between 1 and 5")
        @Max(value = 5, message = "Rating must be between 1 and 5")
        short rating,

        @NotBlank(message = "Message is required")
        @Size(max = 2000, message = "Message must be at most 2000 characters")
        String message,

        boolean isFeatured,

        boolean isActive

) {
}
