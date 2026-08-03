package com.tourpackage.api.dto.request;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateInquiryRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,

        @Size(max = 20, message = "Phone must be at most 20 characters")
        String phone,

        /** Optional — a general question doesn't have a travel date. */
        @FutureOrPresent(message = "Travel date cannot be in the past")
        LocalDate travelDate,

        @Positive(message = "Party size must be greater than 0")
        Short partySize,

        @NotBlank(message = "Message is required")
        @Size(max = 5000, message = "Message must be at most 5000 characters")
        String message,

        /** Set when the visitor came from a package's "Ask about this trip" link. */
        UUID packageId

) {
}
