package com.tourpackage.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GalleryImageRequest(

        @NotBlank(message = "Image URL is required")
        String url,

        @Size(max = 255, message = "Alt text must be at most 255 characters")
        String altText,

        @Size(max = 300, message = "Caption must be at most 300 characters")
        String caption,

        @NotBlank(message = "Category is required")
        @Size(max = 80, message = "Category must be at most 80 characters")
        String category,

        int displayOrder,

        boolean isActive

) {
}
