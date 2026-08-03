package com.tourpackage.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tourpackage.api.entity.BookingTraveller;

public interface BookingTravellerRepository extends JpaRepository<BookingTraveller, UUID> {

    List<BookingTraveller> findByBookingIdOrderByLeadTravellerDescFullNameAsc(UUID bookingId);

}
