package com.tourpackage.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PageSeoRequest(

        @NotBlank(message = "Path is required")
        @Size(max = 200, message = "Path must be at most 200 characters")
        @Pattern(regexp = "^/.*", message = "Path must start with /")
        String path,

        @NotBlank(message = "Meta title is required")
        @Size(max = 200, message = "Meta title must be at most 200 characters")
        String metaTitle,

        @Size(max = 500, message = "Meta description must be at most 500 characters")
        String metaDescription,

        String ogImageUrl,

        boolean noIndex

) {
}
