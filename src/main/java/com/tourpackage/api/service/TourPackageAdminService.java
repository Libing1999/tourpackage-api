package com.tourpackage.api.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tourpackage.api.dto.request.PackageImageRequest;
import com.tourpackage.api.dto.request.PackageItineraryRequest;
import com.tourpackage.api.dto.request.PackageLineItemRequest;
import com.tourpackage.api.dto.request.TourPackageRequest;
import com.tourpackage.api.dto.response.PackageImageResponse;
import com.tourpackage.api.dto.response.PackageItineraryResponse;
import com.tourpackage.api.dto.response.PackageLineItemResponse;
import com.tourpackage.api.dto.response.PageResponse;
import com.tourpackage.api.dto.response.TourPackageAdminListResponse;
import com.tourpackage.api.dto.response.TourPackageAdminResponse;
import com.tourpackage.api.entity.City;
import com.tourpackage.api.entity.ContentStatus;
import com.tourpackage.api.entity.Country;
import com.tourpackage.api.entity.PackageExclude;
import com.tourpackage.api.entity.PackageImage;
import com.tourpackage.api.entity.PackageInclude;
import com.tourpackage.api.entity.PackageItinerary;
import com.tourpackage.api.entity.TourPackage;
import com.tourpackage.api.exception.ApiException;
import com.tourpackage.api.exception.DuplicateResourceException;
import com.tourpackage.api.exception.ResourceNotFoundException;
import com.tourpackage.api.mapper.PackageImageMapper;
import com.tourpackage.api.mapper.PackageLineItemMapper;
import com.tourpackage.api.repository.CityRepository;
import com.tourpackage.api.repository.CountryRepository;
import com.tourpackage.api.repository.PackageExcludeRepository;
import com.tourpackage.api.repository.PackageImageRepository;
import com.tourpackage.api.repository.PackageIncludeRepository;
import com.tourpackage.api.repository.PackageItineraryRepository;
import com.tourpackage.api.repository.TourPackageRepository;

/**
 * Images, itinerary days, and include/exclude line items are all managed
 * through their own nested endpoints rather than as lists on
 * {@link TourPackageRequest} — a package is created first, then its content is
 * built up. Mirrors {@link HotelAdminService}.
 */
@Service
@Transactional
public class TourPackageAdminService {

    private final TourPackageRepository tourPackageRepository;
    private final PackageImageRepository packageImageRepository;
    private final PackageItineraryRepository packageItineraryRepository;
    private final PackageIncludeRepository packageIncludeRepository;
    private final PackageExcludeRepository packageExcludeRepository;
    private final CityRepository cityRepository;
    private final CountryRepository countryRepository;
    private final PackageImageMapper packageImageMapper;
    private final PackageLineItemMapper packageLineItemMapper;

    public TourPackageAdminService(
            TourPackageRepository tourPackageRepository,
            PackageImageRepository packageImageRepository,
            PackageItineraryRepository packageItineraryRepository,
            PackageIncludeRepository packageIncludeRepository,
            PackageExcludeRepository packageExcludeRepository,
            CityRepository cityRepository,
            CountryRepository countryRepository,
            PackageImageMapper packageImageMapper,
            PackageLineItemMapper packageLineItemMapper) {
        this.tourPackageRepository = tourPackageRepository;
        this.packageImageRepository = packageImageRepository;
        this.packageItineraryRepository = packageItineraryRepository;
        this.packageIncludeRepository = packageIncludeRepository;
        this.packageExcludeRepository = packageExcludeRepository;
        this.cityRepository = cityRepository;
        this.countryRepository = countryRepository;
        this.packageImageMapper = packageImageMapper;
        this.packageLineItemMapper = packageLineItemMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<TourPackageAdminListResponse> list(
            ContentStatus status, UUID cityId, String search, Pageable pageable) {
        return PageResponse.of(tourPackageRepository.searchAdmin(status, cityId, search, pageable));
    }

    @Transactional(readOnly = true)
    public TourPackageAdminResponse getById(UUID id) {
        return buildAdminResponse(findPackageOrThrow(id));
    }

    public TourPackageAdminResponse create(TourPackageRequest request) {
        validateReferences(request);

        if (tourPackageRepository.existsBySlugAndDeletedAtIsNull(request.slug())) {
            throw new DuplicateResourceException("A tour package with slug '" + request.slug() + "' already exists");
        }

        Instant now = Instant.now();
        TourPackage tourPackage = TourPackage.builder()
                .title(request.title())
                .slug(request.slug())
                .summary(request.summary())
                .description(request.description())
                .countryId(request.countryId())
                .cityId(request.cityId())
                .durationDays(request.durationDays())
                .durationNights(request.durationNights())
                .price(request.price())
                .discountPrice(request.discountPrice())
                .currencyCode(request.currencyCode())
                .minGroupSize(request.minGroupSize())
                .maxGroupSize(request.maxGroupSize())
                .difficultyLevel(request.difficultyLevel())
                .ratingAverage(BigDecimal.ZERO)
                .ratingCount(0)
                .featured(request.isFeatured())
                .status(request.status())
                .metaTitle(request.metaTitle())
                .metaDescription(request.metaDescription())
                .createdAt(now)
                .updatedAt(now)
                .build();

        return buildAdminResponse(tourPackageRepository.save(tourPackage));
    }

    public TourPackageAdminResponse update(UUID id, TourPackageRequest request) {
        TourPackage tourPackage = findPackageOrThrow(id);
        validateReferences(request);

        if (!tourPackage.getSlug().equals(request.slug())
                && tourPackageRepository.existsBySlugAndIdNotAndDeletedAtIsNull(request.slug(), id)) {
            throw new DuplicateResourceException("A tour package with slug '" + request.slug() + "' already exists");
        }

        tourPackage.setTitle(request.title());
        tourPackage.setSlug(request.slug());
        tourPackage.setSummary(request.summary());
        tourPackage.setDescription(request.description());
        tourPackage.setCountryId(request.countryId());
        tourPackage.setCityId(request.cityId());
        tourPackage.setDurationDays(request.durationDays());
        tourPackage.setDurationNights(request.durationNights());
        tourPackage.setPrice(request.price());
        tourPackage.setDiscountPrice(request.discountPrice());
        tourPackage.setCurrencyCode(request.currencyCode());
        tourPackage.setMinGroupSize(request.minGroupSize());
        tourPackage.setMaxGroupSize(request.maxGroupSize());
        tourPackage.setDifficultyLevel(request.difficultyLevel());
        tourPackage.setFeatured(request.isFeatured());
        tourPackage.setStatus(request.status());
        tourPackage.setMetaTitle(request.metaTitle());
        tourPackage.setMetaDescription(request.metaDescription());
        tourPackage.setUpdatedAt(Instant.now());

        return buildAdminResponse(tourPackageRepository.save(tourPackage));
    }

    public void delete(UUID id) {
        TourPackage tourPackage = findPackageOrThrow(id);
        tourPackage.setDeletedAt(Instant.now());
        tourPackageRepository.save(tourPackage);
    }

    public PackageImageResponse addImage(UUID packageId, PackageImageRequest request) {
        findPackageOrThrow(packageId);

        if (request.isCover()) {
            packageImageRepository.clearCoverForPackage(packageId);
        }

        Instant now = Instant.now();
        PackageImage image = PackageImage.builder()
                .packageId(packageId)
                .url(request.url())
                .altText(request.altText())
                .caption(request.caption())
                .displayOrder(request.displayOrder())
                .cover(request.isCover())
                .createdAt(now)
                .updatedAt(now)
                .build();

        return packageImageMapper.toResponse(packageImageRepository.save(image));
    }

    public PackageImageResponse updateImage(UUID packageId, UUID imageId, PackageImageRequest request) {
        PackageImage image = packageImageRepository.findByIdAndPackageId(imageId, packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Package image not found: " + imageId));

        if (request.isCover() && !image.isCover()) {
            packageImageRepository.clearCoverForPackage(packageId);
        }

        image.setUrl(request.url());
        image.setAltText(request.altText());
        image.setCaption(request.caption());
        image.setDisplayOrder(request.displayOrder());
        image.setCover(request.isCover());
        image.setUpdatedAt(Instant.now());

        return packageImageMapper.toResponse(packageImageRepository.save(image));
    }

    public void deleteImage(UUID packageId, UUID imageId) {
        packageImageRepository.delete(packageImageRepository.findByIdAndPackageId(imageId, packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Package image not found: " + imageId)));
    }

    public PackageItineraryResponse addItineraryDay(UUID packageId, PackageItineraryRequest request) {
        findPackageOrThrow(packageId);
        validateItineraryCity(request.cityId());

        if (packageItineraryRepository.existsByPackageIdAndDayNumber(packageId, request.dayNumber())) {
            throw new DuplicateResourceException("Day " + request.dayNumber() + " already exists for this package");
        }

        Instant now = Instant.now();
        PackageItinerary day = PackageItinerary.builder()
                .packageId(packageId)
                .dayNumber(request.dayNumber())
                .title(request.title())
                .description(request.description())
                .cityId(request.cityId())
                .meals(request.meals())
                .accommodation(request.accommodation())
                .createdAt(now)
                .updatedAt(now)
                .build();

        return toItineraryResponse(packageItineraryRepository.save(day));
    }

    public PackageItineraryResponse updateItineraryDay(UUID packageId, UUID dayId, PackageItineraryRequest request) {
        PackageItinerary day = packageItineraryRepository.findByIdAndPackageId(dayId, packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary day not found: " + dayId));
        validateItineraryCity(request.cityId());

        if (packageItineraryRepository.existsByPackageIdAndDayNumberAndIdNot(packageId, request.dayNumber(), dayId)) {
            throw new DuplicateResourceException("Day " + request.dayNumber() + " already exists for this package");
        }

        day.setDayNumber(request.dayNumber());
        day.setTitle(request.title());
        day.setDescription(request.description());
        day.setCityId(request.cityId());
        day.setMeals(request.meals());
        day.setAccommodation(request.accommodation());
        day.setUpdatedAt(Instant.now());

        return toItineraryResponse(packageItineraryRepository.save(day));
    }

    public void deleteItineraryDay(UUID packageId, UUID dayId) {
        packageItineraryRepository.delete(packageItineraryRepository.findByIdAndPackageId(dayId, packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary day not found: " + dayId)));
    }

    public PackageLineItemResponse addInclude(UUID packageId, PackageLineItemRequest request) {
        findPackageOrThrow(packageId);

        PackageInclude include = PackageInclude.builder()
                .packageId(packageId)
                .description(request.description())
                .icon(request.icon())
                .displayOrder(request.displayOrder())
                .createdAt(Instant.now())
                .build();

        return packageLineItemMapper.toResponse(packageIncludeRepository.save(include));
    }

    public void deleteInclude(UUID packageId, UUID includeId) {
        packageIncludeRepository.delete(packageIncludeRepository.findByIdAndPackageId(includeId, packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Include item not found: " + includeId)));
    }

    public PackageLineItemResponse addExclude(UUID packageId, PackageLineItemRequest request) {
        findPackageOrThrow(packageId);

        PackageExclude exclude = PackageExclude.builder()
                .packageId(packageId)
                .description(request.description())
                .icon(request.icon())
                .displayOrder(request.displayOrder())
                .createdAt(Instant.now())
                .build();

        return packageLineItemMapper.toResponse(packageExcludeRepository.save(exclude));
    }

    public void deleteExclude(UUID packageId, UUID excludeId) {
        packageExcludeRepository.delete(packageExcludeRepository.findByIdAndPackageId(excludeId, packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Exclude item not found: " + excludeId)));
    }

    private TourPackageAdminResponse buildAdminResponse(TourPackage tourPackage) {
        City city = findCityOrThrow(tourPackage.getCityId());
        Country country = countryRepository.findById(tourPackage.getCountryId())
                .orElseThrow(() -> new ResourceNotFoundException("Country not found: " + tourPackage.getCountryId()));

        return new TourPackageAdminResponse(
                tourPackage.getId(),
                tourPackage.getTitle(),
                tourPackage.getSlug(),
                tourPackage.getSummary(),
                tourPackage.getDescription(),
                tourPackage.getCountryId(),
                country.getName(),
                tourPackage.getCityId(),
                city.getName(),
                tourPackage.getDurationDays(),
                tourPackage.getDurationNights(),
                tourPackage.getPrice(),
                tourPackage.getDiscountPrice(),
                tourPackage.getCurrencyCode(),
                tourPackage.getMinGroupSize(),
                tourPackage.getMaxGroupSize(),
                tourPackage.getDifficultyLevel(),
                tourPackage.getRatingAverage(),
                tourPackage.getRatingCount(),
                tourPackage.isFeatured(),
                tourPackage.getStatus(),
                tourPackage.getMetaTitle(),
                tourPackage.getMetaDescription(),
                tourPackage.getCreatedAt(),
                tourPackage.getUpdatedAt(),
                buildImages(tourPackage.getId()),
                buildItinerary(tourPackage.getId()),
                buildIncludes(tourPackage.getId()),
                buildExcludes(tourPackage.getId()));
    }

    List<PackageImageResponse> buildImages(UUID packageId) {
        return packageImageRepository.findByPackageIdOrderByDisplayOrderAsc(packageId).stream()
                .map(packageImageMapper::toResponse)
                .toList();
    }

    List<PackageItineraryResponse> buildItinerary(UUID packageId) {
        List<PackageItinerary> days = packageItineraryRepository.findByPackageIdOrderByDayNumberAsc(packageId);

        if (days.isEmpty()) {
            return List.of();
        }

        Map<UUID, String> cityNames = cityRepository
                .findAllById(days.stream().map(PackageItinerary::getCityId).filter(java.util.Objects::nonNull).distinct().toList())
                .stream()
                .collect(Collectors.toMap(City::getId, City::getName));

        return days.stream()
                .map(day -> new PackageItineraryResponse(
                        day.getId(),
                        day.getDayNumber(),
                        day.getTitle(),
                        day.getDescription(),
                        day.getCityId(),
                        day.getCityId() == null ? null : cityNames.get(day.getCityId()),
                        day.getMeals(),
                        day.getAccommodation()))
                .toList();
    }

    List<PackageLineItemResponse> buildIncludes(UUID packageId) {
        return packageIncludeRepository.findByPackageIdOrderByDisplayOrderAsc(packageId).stream()
                .map(packageLineItemMapper::toResponse)
                .toList();
    }

    List<PackageLineItemResponse> buildExcludes(UUID packageId) {
        return packageExcludeRepository.findByPackageIdOrderByDisplayOrderAsc(packageId).stream()
                .map(packageLineItemMapper::toResponse)
                .toList();
    }

    private PackageItineraryResponse toItineraryResponse(PackageItinerary day) {
        String cityName = day.getCityId() == null
                ? null
                : cityRepository.findById(day.getCityId()).map(City::getName).orElse(null);

        return new PackageItineraryResponse(
                day.getId(),
                day.getDayNumber(),
                day.getTitle(),
                day.getDescription(),
                day.getCityId(),
                cityName,
                day.getMeals(),
                day.getAccommodation());
    }

    private void validateReferences(TourPackageRequest request) {
        findCityOrThrow(request.cityId());

        if (!countryRepository.existsById(request.countryId())) {
            throw new ResourceNotFoundException("Country not found: " + request.countryId());
        }

        // The DB enforces this too (ck_tour_packages_discount_price), but a
        // constraint violation surfaces as an opaque 500 — catching it here
        // gives the client a 400 naming the actual problem.
        if (request.discountPrice() != null && request.discountPrice().compareTo(request.price()) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Discount price cannot be greater than the regular price");
        }

        if (request.maxGroupSize() != null && request.maxGroupSize() < request.minGroupSize()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Maximum group size cannot be less than the minimum");
        }
    }

    private void validateItineraryCity(UUID cityId) {
        if (cityId != null) {
            findCityOrThrow(cityId);
        }
    }

    private TourPackage findPackageOrThrow(UUID id) {
        return tourPackageRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tour package not found: " + id));
    }

    private City findCityOrThrow(UUID cityId) {
        return cityRepository.findById(cityId)
                .orElseThrow(() -> new ResourceNotFoundException("City not found: " + cityId));
    }

}
