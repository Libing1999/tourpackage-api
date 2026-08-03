package com.tourpackage.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record NewsletterSubscriberResponse(
        UUID id,
        String email,
        boolean isActive,
        Instant subscribedAt,
        Instant unsubscribedAt
) {
}
