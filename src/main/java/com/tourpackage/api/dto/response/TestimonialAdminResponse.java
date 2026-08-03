package com.tourpackage.api.dto.response;

import java.time.Instant;
import java.util.UUID;

/** Distinct from the public {@code TestimonialResponse}, which deliberately
 * omits the moderation flags a visitor has no business seeing. */
public record TestimonialAdminResponse(
        UUID id,
        String customerName,
        String customerAvatarUrl,
        UUID customerCountryId,
        String customerCountryName,
        UUID packageId,
        String packageTitle,
        short rating,
        String message,
        boolean isFeatured,
        boolean isActive,
        Instant createdAt
) {
}
