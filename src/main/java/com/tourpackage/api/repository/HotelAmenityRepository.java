package com.tourpackage.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tourpackage.api.entity.HotelAmenity;

public interface HotelAmenityRepository extends JpaRepository<HotelAmenity, UUID> {

    List<HotelAmenity> findByHotelId(UUID hotelId);

    @Modifying
    @Query("DELETE FROM HotelAmenity ha WHERE ha.hotelId = :hotelId")
    void deleteByHotelId(@Param("hotelId") UUID hotelId);

}
