package com.tourpackage.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(

        @NotBlank(message = "Full name is required")
        @Size(max = 150, message = "Full name must be at most 150 characters")
        String fullName,

        @Size(max = 20, message = "Phone must be at most 20 characters")
        @Pattern(regexp = "^$|^[+0-9()\\-\\s]{6,20}$", message = "Phone must be a valid phone number")
        String phone,

        @Size(max = 2048, message = "Avatar URL must be at most 2048 characters")
        String avatarUrl

) {
}
