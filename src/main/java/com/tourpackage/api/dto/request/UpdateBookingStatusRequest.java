package com.tourpackage.api.dto.request;

import com.tourpackage.api.entity.BookingStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateBookingStatusRequest(

        @NotNull(message = "Status is required")
        BookingStatus status,

        @Size(max = 500, message = "Cancellation reason must be at most 500 characters")
        String cancellationReason

) {
}
