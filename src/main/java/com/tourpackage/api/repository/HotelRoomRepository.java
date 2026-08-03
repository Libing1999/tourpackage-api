package com.tourpackage.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tourpackage.api.entity.HotelRoom;

public interface HotelRoomRepository extends JpaRepository<HotelRoom, UUID> {

    List<HotelRoom> findByHotelIdOrderByPricePerNightAsc(UUID hotelId);

    // Scoped by hotelId as well as id so an admin can't update/delete a room
    // by guessing its UUID for a hotel they weren't given — every nested
    // room endpoint looks the room up this way, never by id alone.
    Optional<HotelRoom> findByIdAndHotelId(UUID id, UUID hotelId);

    boolean existsByRoomTypeId(UUID roomTypeId);

}
