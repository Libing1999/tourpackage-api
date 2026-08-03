package com.tourpackage.api.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tourpackage.api.dto.response.HotelPublicDetailResponse;
import com.tourpackage.api.dto.response.HotelSummaryResponse;
import com.tourpackage.api.dto.response.PageResponse;
import com.tourpackage.api.entity.City;
import com.tourpackage.api.entity.ContentStatus;
import com.tourpackage.api.entity.Country;
import com.tourpackage.api.entity.Hotel;
import com.tourpackage.api.exception.ResourceNotFoundException;
import com.tourpackage.api.repository.CityRepository;
import com.tourpackage.api.repository.CountryRepository;
import com.tourpackage.api.repository.HotelRepository;

@Service
@Transactional(readOnly = true)
public class HotelService {

    private final HotelRepository hotelRepository;
    private final CityRepository cityRepository;
    private final CountryRepository countryRepository;
    private final HotelAdminService hotelAdminService;

    public HotelService(
            HotelRepository hotelRepository,
            CityRepository cityRepository,
            CountryRepository countryRepository,
            HotelAdminService hotelAdminService) {
        this.hotelRepository = hotelRepository;
        this.cityRepository = cityRepository;
        this.countryRepository = countryRepository;
        this.hotelAdminService = hotelAdminService;
    }

    public List<HotelSummaryResponse> getTopHotels(int limit) {
        return hotelRepository.findTopHotels(PageRequest.of(0, limit));
    }

    public PageResponse<HotelSummaryResponse> list(
            UUID cityId,
            UUID countryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Short minStarRating,
            String search,
            List<UUID> amenityIds,
            Pageable pageable) {
        List<UUID> ids = amenityIds == null ? List.of() : amenityIds;

        Page<HotelSummaryResponse> page = hotelRepository.searchPublic(
                cityId, countryId, minPrice, maxPrice, minStarRating, search, ids, ids.size(), pageable);

        return PageResponse.of(page);
    }

    public HotelPublicDetailResponse getBySlug(String slug) {
        Hotel hotel = hotelRepository.findBySlugAndDeletedAtIsNullAndStatus(slug, ContentStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found: " + slug));

        City city = cityRepository.findById(hotel.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City not found: " + hotel.getCityId()));
        Country country = countryRepository.findById(city.getCountryId())
                .orElseThrow(() -> new ResourceNotFoundException("Country not found: " + city.getCountryId()));

        return new HotelPublicDetailResponse(
                hotel.getId(),
                hotel.getName(),
                hotel.getSlug(),
                hotel.getDescription(),
                hotel.getShortDescription(),
                hotel.getStarRating(),
                city.getName(),
                country.getName(),
                hotel.getAddressLine1(),
                hotel.getAddressLine2(),
                hotel.getPostalCode(),
                hotel.getLatitude(),
                hotel.getLongitude(),
                hotel.getContactEmail(),
                hotel.getContactPhone(),
                hotel.getWebsiteUrl(),
                hotel.getCheckInTime(),
                hotel.getCheckOutTime(),
                hotel.getBasePrice(),
                hotel.getCurrencyCode(),
                hotel.getRatingAverage(),
                hotel.getRatingCount(),
                hotel.getMetaTitle(),
                hotel.getMetaDescription(),
                hotelAdminService.buildImages(hotel.getId()),
                hotelAdminService.buildAmenities(hotel.getId()),
                hotelAdminService.buildRooms(hotel.getId()));
    }

}
