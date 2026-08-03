package com.tourpackage.api.dto.request;

import com.tourpackage.api.entity.AmenityCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AmenityRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @NotBlank(message = "Slug is required")
        @Size(max = 120, message = "Slug must be at most 120 characters")
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "Slug must be lowercase-kebab-case")
        String slug,

        @Size(max = 80, message = "Icon must be at most 80 characters")
        String icon,

        @NotNull(message = "Category is required")
        AmenityCategory category,

        int displayOrder,

        boolean isActive

) {
}
