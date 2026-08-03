package com.tourpackage.api.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.tourpackage.api.entity.InquiryStatus;

/** Row shape for the admin inbox — the message is truncated client-side, so
 * the full text still comes along; it's the package join that's skipped. */
public record InquiryAdminListResponse(
        UUID id,
        String name,
        String email,
        String phone,
        LocalDate travelDate,
        Short partySize,
        String message,
        InquiryStatus status,
        String packageTitle,
        Instant createdAt
) {
}
