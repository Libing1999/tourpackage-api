package com.tourpackage.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tourpackage.api.entity.HotelImage;

public interface HotelImageRepository extends JpaRepository<HotelImage, UUID> {

    List<HotelImage> findByHotelIdOrderByDisplayOrderAsc(UUID hotelId);

    Optional<HotelImage> findByIdAndHotelId(UUID id, UUID hotelId);

    // The DB only allows one is_cover=true row per hotel (partial unique
    // index) — call this before saving a row with isCover=true so the
    // insert/update doesn't collide with whatever was previously the cover.
    @Modifying
    @Query("UPDATE HotelImage hi SET hi.cover = false WHERE hi.hotelId = :hotelId AND hi.cover = true")
    void clearCoverForHotel(@Param("hotelId") UUID hotelId);

}
