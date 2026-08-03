package com.tourpackage.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tourpackage.api.entity.Amenity;

public interface AmenityRepository extends JpaRepository<Amenity, UUID> {

    List<Amenity> findAllByOrderByDisplayOrderAscNameAsc();

    List<Amenity> findAllByActiveTrueOrderByDisplayOrderAscNameAsc();

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

}
