package com.tourpackage.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.tourpackage.api.entity.BookingStatus;
import com.tourpackage.api.entity.BookingType;

/**
 * Covers both hotel and package bookings. {@code bookingType} discriminates
 * which of the two blocks below is populated — the other's fields are null.
 * One shape rather than two because booking history returns them interleaved,
 * and a client showing a mixed list shouldn't have to switch on the response
 * type before it can even read the booking number.
 *
 * <p>{@code startDate}/{@code endDate} are the shared {@code travel_date} and
 * {@code return_date} columns; the label differs by type (check-in/check-out
 * for a hotel, departure/return for a package) and belongs to the UI.
 */
public record BookingResponse(
        UUID id,
        String bookingNumber,
        BookingType bookingType,
        BookingStatus status,

        // --- HOTEL only ---
        String hotelName,
        String hotelSlug,
        String roomName,
        String roomTypeName,
        BigDecimal pricePerNight,
        Integer nights,

        // --- PACKAGE only ---
        String packageTitle,
        String packageSlug,
        Short durationDays,
        Short durationNights,
        BigDecimal pricePerAdult,
        BigDecimal pricePerChild,

        // --- common ---
        String cityName,
        String countryName,
        LocalDate startDate,
        LocalDate endDate,
        short numberOfAdults,
        short numberOfChildren,
        BigDecimal totalAmount,
        String currencyCode,

        String guestFullName,
        String guestEmail,
        String guestPhone,

        String specialRequests,
        Instant cancelledAt,
        String cancellationReason,
        Instant createdAt,

        List<BookingTravellerResponse> travellers,
        BookingPaymentResponse payment
) {
}
