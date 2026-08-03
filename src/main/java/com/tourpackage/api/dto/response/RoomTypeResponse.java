package com.tourpackage.api.dto.response;

import java.util.UUID;

public record RoomTypeResponse(
        UUID id,
        String name,
        String slug,
        String description,
        short maxOccupancy,
        int displayOrder,
        boolean isActive
) {
}
