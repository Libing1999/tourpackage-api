package com.tourpackage.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HotelImageRequest(

        @NotBlank(message = "Image URL is required")
        String url,

        @Size(max = 255, message = "Alt text must be at most 255 characters")
        String altText,

        @Size(max = 500, message = "Caption must be at most 500 characters")
        String caption,

        int displayOrder,

        boolean isCover

) {
}
