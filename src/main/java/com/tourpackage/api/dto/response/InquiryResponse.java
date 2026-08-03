package com.tourpackage.api.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.tourpackage.api.entity.InquiryStatus;

public record InquiryResponse(
        UUID id,
        String name,
        String email,
        String phone,
        LocalDate travelDate,
        Short partySize,
        String message,
        InquiryStatus status,
        UUID packageId,
        String packageTitle,
        String packageSlug,
        Instant respondedAt,
        Instant createdAt
) {
}
