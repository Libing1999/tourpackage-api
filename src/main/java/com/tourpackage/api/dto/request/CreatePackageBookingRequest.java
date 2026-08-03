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
 * There's no return date here: a package's length is fixed by its
 * {@code durationDays}, so the guest picks a departure date and the server
 * derives the return. Letting the client send one would just be a second
 * source of truth to disagree with the package.
 */
public record CreatePackageBookingRequest(

        @NotNull(message = "Package is required")
        UUID packageId,

        @NotNull(message = "Travel date is required")
        @FutureOrPresent(message = "Travel date cannot be in the past")
        LocalDate travelDate,

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

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod

) {
}
