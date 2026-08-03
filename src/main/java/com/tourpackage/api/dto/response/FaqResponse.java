package com.tourpackage.api.dto.response;

import java.util.UUID;

public record FaqResponse(
        UUID id,
        String question,
        String answer,
        String category
) {
}
