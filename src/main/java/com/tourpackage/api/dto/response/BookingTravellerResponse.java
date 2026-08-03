package com.tourpackage.api.dto.response;

import java.time.LocalDate;
import java.util.UUID;

import com.tourpackage.api.entity.Gender;

public record BookingTravellerResponse(
        UUID id,
        String fullName,
        LocalDate dateOfBirth,
        Gender gender,
        String passportNumber,
        LocalDate passportExpiry,
        boolean isLeadTraveller
) {
}
