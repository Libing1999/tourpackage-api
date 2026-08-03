package com.tourpackage.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tourpackage.api.entity.PackageItinerary;

public interface PackageItineraryRepository extends JpaRepository<PackageItinerary, UUID> {

    List<PackageItinerary> findByPackageIdOrderByDayNumberAsc(UUID packageId);

    Optional<PackageItinerary> findByIdAndPackageId(UUID id, UUID packageId);

    boolean existsByPackageIdAndDayNumber(UUID packageId, short dayNumber);

    boolean existsByPackageIdAndDayNumberAndIdNot(UUID packageId, short dayNumber, UUID id);

}
