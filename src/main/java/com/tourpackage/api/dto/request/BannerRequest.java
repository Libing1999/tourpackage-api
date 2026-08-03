package com.tourpackage.api.dto.request;

import java.time.Instant;

import com.tourpackage.api.entity.BannerPlacement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BannerRequest(

        @NotNull(message = "Placement is required")
        BannerPlacement placement,

        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @Size(max = 300, message = "Subtitle must be at most 300 characters")
        String subtitle,

        @NotBlank(message = "Image URL is required")
        String imageUrl,

        String linkUrl,

        @Size(max = 50, message = "Button label must be at most 50 characters")
        String buttonLabel,

        int displayOrder,

        boolean isActive,

        Instant startsAt,

        Instant endsAt

) {
}
