package com.tourpackage.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ContentBlockRequest(

        @NotBlank(message = "Key is required")
        @Size(max = 100, message = "Key must be at most 100 characters")
        @Pattern(regexp = "^[a-z0-9]+(\\.[a-z0-9-]+)*$",
                message = "Key must be lowercase dotted, e.g. home.hotels")
        String key,

        @Size(max = 100, message = "Eyebrow must be at most 100 characters")
        String eyebrow,

        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @Size(max = 500, message = "Subtitle must be at most 500 characters")
        String subtitle,

        String body,

        boolean isActive

) {
}
