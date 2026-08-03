package com.tourpackage.api.dto.request;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.tourpackage.api.entity.PaymentMethod;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * The whole booking flow submitted as one request. The frontend collects it
 * across several steps, but there's no reason for the server to hold partial
 * state between them — nothing is persisted until the guest confirms, so a
 * half-finished booking simply doesn't exist rather than sitting in the
 * database as a row nobody will ever complete.
 */
public record CreateHotelBookingRequest(

        @NotNull(message = "Room is required")
        UUID hotelRoomId,

        @NotNull(message = "Check-in date is required")
        @FutureOrPresent(message = "Check-in date cannot be in the past")
        LocalDate checkInDate,

        @NotNull(message = "Check-out date is required")
        @FutureOrPresent(message = "Check-out date cannot be in the past")
        LocalDate checkOutDate,

        @Positive(message = "At least one adult is required")
        short numberOfAdults,

        @PositiveOrZero(message = "Number of children cannot be negative")
        short numberOfChildren,

        @NotNull(message = "Guest details are required")
        @Valid
        GuestDetailsRequest guest,

        @NotEmpty(message = "At least one traveller is required")
        @Valid
        List<TravellerRequest> travellers,

        @Size(max = 1000, message = "Special requests must be at most 1000 characters")
        String specialRequests,

        /** Placeholder — no gateway is wired up, so this only records how the
         * guest intends to pay. See {@code BookingService.recordPlaceholderPayment}. */
        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod

) {
}
