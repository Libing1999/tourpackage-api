package com.tourpackage.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A customer as the admin sees them, with their booking activity rolled up —
 * "who is this and are they worth calling back" in one row. */
public record CustomerResponse(
        UUID id,
        String fullName,
        String email,
        String phone,
        long bookingCount,
        BigDecimal totalSpent,
        Instant createdAt
) {
}
