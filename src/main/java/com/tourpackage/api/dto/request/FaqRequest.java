package com.tourpackage.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FaqRequest(

        @NotBlank(message = "Question is required")
        @Size(max = 300, message = "Question must be at most 300 characters")
        String question,

        @NotBlank(message = "Answer is required")
        String answer,

        @NotBlank(message = "Category is required")
        @Size(max = 100, message = "Category must be at most 100 characters")
        String category,

        int displayOrder,

        boolean isActive

) {
}
