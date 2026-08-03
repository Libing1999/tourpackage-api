package com.tourpackage.api.dto.request;

import java.time.LocalDate;
import java.util.UUID;

import com.tourpackage.api.entity.Gender;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record TravellerRequest(

        @NotBlank(message = "Full name is required")
        @Size(max = 150, message = "Full name must be at most 150 characters")
        String fullName,

        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        Gender gender,

        @Size(max = 50, message = "Passport number must be at most 50 characters")
        String passportNumber,

        LocalDate passportExpiry,

        UUID nationalityCountryId,

        boolean isLeadTraveller

) {
}
