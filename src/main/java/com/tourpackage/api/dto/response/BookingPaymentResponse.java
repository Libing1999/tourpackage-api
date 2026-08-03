package com.tourpackage.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.tourpackage.api.entity.PaymentMethod;
import com.tourpackage.api.entity.PaymentStatus;

public record BookingPaymentResponse(
        UUID id,
        BigDecimal amount,
        String currencyCode,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        Instant paidAt
) {
}
