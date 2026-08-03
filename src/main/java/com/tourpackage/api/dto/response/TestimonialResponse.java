package com.tourpackage.api.dto.response;

import java.util.UUID;

public record TestimonialResponse(
        UUID id,
        String customerName,
        String customerAvatarUrl,
        String customerCountryName,
        short rating,
        String message,
        String packageTitle
) {
}
