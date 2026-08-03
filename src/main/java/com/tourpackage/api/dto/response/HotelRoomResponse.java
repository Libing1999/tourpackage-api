package com.tourpackage.api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record HotelRoomResponse(
        UUID id,
        UUID roomTypeId,
        String roomTypeName,
        String name,
        String description,
        short maxAdults,
        short maxChildren,
        short bedCount,
        String bedType,
        BigDecimal sizeSqm,
        BigDecimal pricePerNight,
        String currencyCode,
        int totalRooms,
        boolean isActive,
        boolean isAvailable
) {
}
