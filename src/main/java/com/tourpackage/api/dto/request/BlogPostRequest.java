package com.tourpackage.api.dto.request;

import com.tourpackage.api.entity.ContentStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record BlogPostRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @NotBlank(message = "Slug is required")
        @Size(max = 220, message = "Slug must be at most 220 characters")
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "Slug must be lowercase-kebab-case")
        String slug,

        @Size(max = 500, message = "Excerpt must be at most 500 characters")
        String excerpt,

        @NotBlank(message = "Content is required")
        String content,

        String coverImageUrl,

        @NotBlank(message = "Category is required")
        @Size(max = 100, message = "Category must be at most 100 characters")
        String category,

        @NotNull(message = "Status is required")
        ContentStatus status,

        @Positive(message = "Read time must be greater than 0")
        Short readTimeMinutes

) {
}
