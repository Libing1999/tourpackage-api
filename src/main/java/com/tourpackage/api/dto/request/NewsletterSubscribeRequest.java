package com.tourpackage.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record NewsletterSubscribeRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        String email

) {
}
