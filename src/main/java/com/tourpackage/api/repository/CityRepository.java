package com.tourpackage.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.tourpackage.api.dto.response.DestinationResponse;
import com.tourpackage.api.entity.City;

public interface CityRepository extends JpaRepository<City, UUID> {

    // packageCount is a 0 placeholder — CityRepository has no business
    // knowing about tour_packages. DestinationService fills in the real
    // count from TourPackageRepository.countPublishedByCity and rebuilds
    // the record (DestinationResponse is immutable, so "filling in" means
    // constructing a new instance, not mutating this one).
    @Query("""
            SELECT new com.tourpackage.api.dto.response.DestinationResponse(
                c.id, c.name, c.slug, co.name, c.imageUrl, 0L)
            FROM City c JOIN Country co ON co.id = c.countryId
            WHERE c.popular = true AND c.active = true
            ORDER BY c.name ASC
            """)
    List<DestinationResponse> findPopularDestinations(Pageable pageable);

    // Every active city, for filter dropdowns. packageCount is a 0 placeholder
    // for the same reason as above — DestinationService fills it in.
    @Query("""
            SELECT new com.tourpackage.api.dto.response.DestinationResponse(
                c.id, c.name, c.slug, co.name, c.imageUrl, 0L)
            FROM City c JOIN Country co ON co.id = c.countryId
            WHERE c.active = true
            ORDER BY c.name ASC
            """)
    List<DestinationResponse> findAllActiveDestinations();

}
