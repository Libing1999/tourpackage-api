package com.tourpackage.api.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tourpackage.api.entity.BookingPayment;

public interface BookingPaymentRepository extends JpaRepository<BookingPayment, UUID> {

    Optional<BookingPayment> findFirstByBookingIdOrderByCreatedAtDesc(UUID bookingId);

}
