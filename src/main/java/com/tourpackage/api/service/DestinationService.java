package com.tourpackage.api.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tourpackage.api.dto.response.DestinationResponse;
import com.tourpackage.api.repository.CityRepository;
import com.tourpackage.api.repository.TourPackageRepository;

@Service
@Transactional(readOnly = true)
public class DestinationService {

    private final CityRepository cityRepository;
    private final TourPackageRepository tourPackageRepository;

    public DestinationService(CityRepository cityRepository, TourPackageRepository tourPackageRepository) {
        this.cityRepository = cityRepository;
        this.tourPackageRepository = tourPackageRepository;
    }

    public List<DestinationResponse> getPopularDestinations(int limit) {
        return withPackageCounts(cityRepository.findPopularDestinations(PageRequest.of(0, limit)));
    }

    /** Every active city — backs the "destination" filter dropdown on the
     * package/hotel listing pages, which needs the full list rather than the
     * popular-only subset the homepage shows. */
    public List<DestinationResponse> getAllDestinations() {
        return withPackageCounts(cityRepository.findAllActiveDestinations());
    }

    private List<DestinationResponse> withPackageCounts(List<DestinationResponse> destinations) {
        Map<UUID, Long> packageCountsByCity = tourPackageRepository.countPublishedByCity().stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]));

        // DestinationResponse is a record — "filling in" the count means
        // building a new instance from the query's placeholder row, not
        // mutating it.
        return destinations.stream()
                .map(d -> new DestinationResponse(
                        d.id(), d.name(), d.slug(), d.countryName(), d.imageUrl(),
                        packageCountsByCity.getOrDefault(d.id(), 0L)))
                .toList();
    }

}
