package com.tourpackage.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.tourpackage.api.entity.BookingStatus;
import com.tourpackage.api.entity.BookingType;

/** Row-level shape for the paginated admin bookings table — no travellers or
 * payment, same rationale as {@link HotelAdminListResponse}. */
public record BookingAdminListResponse(
        UUID id,
        String bookingNumber,
        BookingType bookingType,
        BookingStatus status,
        String hotelName,
        String roomName,
        String guestFullName,
        String guestEmail,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        BigDecimal totalAmount,
        String currencyCode,
        Instant createdAt
) {
}
