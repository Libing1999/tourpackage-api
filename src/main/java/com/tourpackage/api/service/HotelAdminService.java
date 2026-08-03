package com.tourpackage.api.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tourpackage.api.dto.request.HotelImageRequest;
import com.tourpackage.api.dto.request.HotelRequest;
import com.tourpackage.api.dto.request.HotelRoomRequest;
import com.tourpackage.api.dto.response.AmenityResponse;
import com.tourpackage.api.dto.response.HotelAdminListResponse;
import com.tourpackage.api.dto.response.HotelAdminResponse;
import com.tourpackage.api.dto.response.HotelImageResponse;
import com.tourpackage.api.dto.response.HotelRoomResponse;
import com.tourpackage.api.dto.response.PageResponse;
import com.tourpackage.api.entity.City;
import com.tourpackage.api.entity.ContentStatus;
import com.tourpackage.api.entity.Country;
import com.tourpackage.api.entity.Hotel;
import com.tourpackage.api.entity.HotelAmenity;
import com.tourpackage.api.entity.HotelImage;
import com.tourpackage.api.entity.HotelRoom;
import com.tourpackage.api.entity.RoomType;
import com.tourpackage.api.exception.DuplicateResourceException;
import com.tourpackage.api.exception.ResourceNotFoundException;
import com.tourpackage.api.mapper.AmenityMapper;
import com.tourpackage.api.mapper.HotelImageMapper;
import com.tourpackage.api.repository.AmenityRepository;
import com.tourpackage.api.repository.CityRepository;
import com.tourpackage.api.repository.CountryRepository;
import com.tourpackage.api.repository.HotelAmenityRepository;
import com.tourpackage.api.repository.HotelImageRepository;
import com.tourpackage.api.repository.HotelRepository;
import com.tourpackage.api.repository.HotelRoomRepository;
import com.tourpackage.api.repository.RoomTypeRepository;

/**
 * Images and rooms are managed through their own nested endpoints rather
 * than as replace-all lists on {@link #create}/{@link #update} — a hotel is
 * created first, then photos/rooms are added afterwards. Amenities are the
 * exception: they're a plain many-to-many set, so {@code amenityIds} on
 * {@link HotelRequest} is always applied as replace-all.
 */
@Service
@Transactional
public class HotelAdminService {

    private final HotelRepository hotelRepository;
    private final HotelImageRepository hotelImageRepository;
    private final HotelRoomRepository hotelRoomRepository;
    private final HotelAmenityRepository hotelAmenityRepository;
    private final AmenityRepository amenityRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final CityRepository cityRepository;
    private final CountryRepository countryRepository;
    private final HotelImageMapper hotelImageMapper;
    private final AmenityMapper amenityMapper;

    public HotelAdminService(
            HotelRepository hotelRepository,
            HotelImageRepository hotelImageRepository,
            HotelRoomRepository hotelRoomRepository,
            HotelAmenityRepository hotelAmenityRepository,
            AmenityRepository amenityRepository,
            RoomTypeRepository roomTypeRepository,
            CityRepository cityRepository,
            CountryRepository countryRepository,
            HotelImageMapper hotelImageMapper,
            AmenityMapper amenityMapper) {
        this.hotelRepository = hotelRepository;
        this.hotelImageRepository = hotelImageRepository;
        this.hotelRoomRepository = hotelRoomRepository;
        this.hotelAmenityRepository = hotelAmenityRepository;
        this.amenityRepository = amenityRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.cityRepository = cityRepository;
        this.countryRepository = countryRepository;
        this.hotelImageMapper = hotelImageMapper;
        this.amenityMapper = amenityMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<HotelAdminListResponse> list(ContentStatus status, UUID cityId, String search, Pageable pageable) {
        return PageResponse.of(hotelRepository.searchAdmin(status, cityId, search, pageable));
    }

    @Transactional(readOnly = true)
    public HotelAdminResponse getById(UUID id) {
        return buildAdminResponse(findHotelOrThrow(id));
    }

    public HotelAdminResponse create(HotelRequest request) {
        findCityOrThrow(request.cityId());

        if (hotelRepository.existsBySlugAndDeletedAtIsNull(request.slug())) {
            throw new DuplicateResourceException("A hotel with slug '" + request.slug() + "' already exists");
        }

        Instant now = Instant.now();
        Hotel hotel = Hotel.builder()
                .name(request.name())
                .slug(request.slug())
                .description(request.description())
                .shortDescription(request.shortDescription())
                .starRating(request.starRating())
                .cityId(request.cityId())
                .addressLine1(request.addressLine1())
                .addressLine2(request.addressLine2())
                .postalCode(request.postalCode())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .contactEmail(request.contactEmail())
                .contactPhone(request.contactPhone())
                .websiteUrl(request.websiteUrl())
                .checkInTime(request.checkInTime())
                .checkOutTime(request.checkOutTime())
                .basePrice(request.basePrice())
                .currencyCode(request.currencyCode())
                .ratingAverage(java.math.BigDecimal.ZERO)
                .ratingCount(0)
                .featured(request.isFeatured())
                .status(request.status())
                .metaTitle(request.metaTitle())
                .metaDescription(request.metaDescription())
                .createdAt(now)
                .updatedAt(now)
                .build();

        hotel = hotelRepository.save(hotel);
        assignAmenities(hotel.getId(), request.amenityIds());

        return buildAdminResponse(hotel);
    }

    public HotelAdminResponse update(UUID id, HotelRequest request) {
        Hotel hotel = findHotelOrThrow(id);
        findCityOrThrow(request.cityId());

        if (!hotel.getSlug().equals(request.slug())
                && hotelRepository.existsBySlugAndIdNotAndDeletedAtIsNull(request.slug(), id)) {
            throw new DuplicateResourceException("A hotel with slug '" + request.slug() + "' already exists");
        }

        hotel.setName(request.name());
        hotel.setSlug(request.slug());
        hotel.setDescription(request.description());
        hotel.setShortDescription(request.shortDescription());
        hotel.setStarRating(request.starRating());
        hotel.setCityId(request.cityId());
        hotel.setAddressLine1(request.addressLine1());
        hotel.setAddressLine2(request.addressLine2());
        hotel.setPostalCode(request.postalCode());
        hotel.setLatitude(request.latitude());
        hotel.setLongitude(request.longitude());
        hotel.setContactEmail(request.contactEmail());
        hotel.setContactPhone(request.contactPhone());
        hotel.setWebsiteUrl(request.websiteUrl());
        hotel.setCheckInTime(request.checkInTime());
        hotel.setCheckOutTime(request.checkOutTime());
        hotel.setBasePrice(request.basePrice());
        hotel.setCurrencyCode(request.currencyCode());
        hotel.setFeatured(request.isFeatured());
        hotel.setStatus(request.status());
        hotel.setMetaTitle(request.metaTitle());
        hotel.setMetaDescription(request.metaDescription());
        hotel.setUpdatedAt(Instant.now());

        hotel = hotelRepository.save(hotel);
        assignAmenities(hotel.getId(), request.amenityIds());

        return buildAdminResponse(hotel);
    }

    public void delete(UUID id) {
        Hotel hotel = findHotelOrThrow(id);
        hotel.setDeletedAt(Instant.now());
        hotelRepository.save(hotel);
    }

    public HotelImageResponse addImage(UUID hotelId, HotelImageRequest request) {
        findHotelOrThrow(hotelId);

        if (request.isCover()) {
            hotelImageRepository.clearCoverForHotel(hotelId);
        }

        Instant now = Instant.now();
        HotelImage image = HotelImage.builder()
                .hotelId(hotelId)
                .url(request.url())
                .altText(request.altText())
                .caption(request.caption())
                .displayOrder(request.displayOrder())
                .cover(request.isCover())
                .createdAt(now)
                .updatedAt(now)
                .build();

        return hotelImageMapper.toResponse(hotelImageRepository.save(image));
    }

    public HotelImageResponse updateImage(UUID hotelId, UUID imageId, HotelImageRequest request) {
        HotelImage image = findImageOrThrow(hotelId, imageId);

        if (request.isCover() && !image.isCover()) {
            hotelImageRepository.clearCoverForHotel(hotelId);
        }

        image.setUrl(request.url());
        image.setAltText(request.altText());
        image.setCaption(request.caption());
        image.setDisplayOrder(request.displayOrder());
        image.setCover(request.isCover());
        image.setUpdatedAt(Instant.now());

        return hotelImageMapper.toResponse(hotelImageRepository.save(image));
    }

    public void deleteImage(UUID hotelId, UUID imageId) {
        hotelImageRepository.delete(findImageOrThrow(hotelId, imageId));
    }

    public HotelRoomResponse addRoom(UUID hotelId, HotelRoomRequest request) {
        findHotelOrThrow(hotelId);
        RoomType roomType = findRoomTypeOrThrow(request.roomTypeId());

        Instant now = Instant.now();
        HotelRoom room = HotelRoom.builder()
                .hotelId(hotelId)
                .roomTypeId(request.roomTypeId())
                .name(request.name())
                .description(request.description())
                .maxAdults(request.maxAdults())
                .maxChildren(request.maxChildren())
                .bedCount(request.bedCount())
                .bedType(request.bedType())
                .sizeSqm(request.sizeSqm())
                .pricePerNight(request.pricePerNight())
                .currencyCode(request.currencyCode())
                .totalRooms(request.totalRooms())
                .active(request.isActive())
                .createdAt(now)
                .updatedAt(now)
                .build();

        return toRoomResponse(hotelRoomRepository.save(room), roomType.getName());
    }

    public HotelRoomResponse updateRoom(UUID hotelId, UUID roomId, HotelRoomRequest request) {
        HotelRoom room = findRoomOrThrow(hotelId, roomId);
        RoomType roomType = findRoomTypeOrThrow(request.roomTypeId());

        room.setRoomTypeId(request.roomTypeId());
        room.setName(request.name());
        room.setDescription(request.description());
        room.setMaxAdults(request.maxAdults());
        room.setMaxChildren(request.maxChildren());
        room.setBedCount(request.bedCount());
        room.setBedType(request.bedType());
        room.setSizeSqm(request.sizeSqm());
        room.setPricePerNight(request.pricePerNight());
        room.setCurrencyCode(request.currencyCode());
        room.setTotalRooms(request.totalRooms());
        room.setActive(request.isActive());
        room.setUpdatedAt(Instant.now());

        return toRoomResponse(hotelRoomRepository.save(room), roomType.getName());
    }

    public void deleteRoom(UUID hotelId, UUID roomId) {
        hotelRoomRepository.delete(findRoomOrThrow(hotelId, roomId));
    }

    private void assignAmenities(UUID hotelId, List<UUID> amenityIds) {
        hotelAmenityRepository.deleteByHotelId(hotelId);

        if (amenityIds == null || amenityIds.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        List<HotelAmenity> rows = amenityIds.stream()
                .distinct()
                .map(amenityId -> HotelAmenity.builder()
                        .hotelId(hotelId)
                        .amenityId(amenityId)
                        .createdAt(now)
                        .build())
                .toList();

        hotelAmenityRepository.saveAll(rows);
    }

    private HotelAdminResponse buildAdminResponse(Hotel hotel) {
        City city = cityRepository.findById(hotel.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City not found: " + hotel.getCityId()));
        Country country = countryRepository.findById(city.getCountryId())
                .orElseThrow(() -> new ResourceNotFoundException("Country not found: " + city.getCountryId()));

        return new HotelAdminResponse(
                hotel.getId(),
                hotel.getName(),
                hotel.getSlug(),
                hotel.getDescription(),
                hotel.getShortDescription(),
                hotel.getStarRating(),
                hotel.getCityId(),
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
                hotel.isFeatured(),
                hotel.getStatus(),
                hotel.getMetaTitle(),
                hotel.getMetaDescription(),
                hotel.getCreatedAt(),
                hotel.getUpdatedAt(),
                buildImages(hotel.getId()),
                buildAmenities(hotel.getId()),
                buildRooms(hotel.getId()));
    }

    List<HotelImageResponse> buildImages(UUID hotelId) {
        return hotelImageRepository.findByHotelIdOrderByDisplayOrderAsc(hotelId).stream()
                .map(hotelImageMapper::toResponse)
                .toList();
    }

    List<AmenityResponse> buildAmenities(UUID hotelId) {
        List<UUID> amenityIds = hotelAmenityRepository.findByHotelId(hotelId).stream()
                .map(HotelAmenity::getAmenityId)
                .toList();

        if (amenityIds.isEmpty()) {
            return List.of();
        }

        return amenityRepository.findAllById(amenityIds).stream()
                .map(amenityMapper::toResponse)
                .sorted(Comparator.comparing(AmenityResponse::displayOrder).thenComparing(AmenityResponse::name))
                .toList();
    }

    List<HotelRoomResponse> buildRooms(UUID hotelId) {
        List<HotelRoom> rooms = hotelRoomRepository.findByHotelIdOrderByPricePerNightAsc(hotelId);

        if (rooms.isEmpty()) {
            return List.of();
        }

        Map<UUID, String> roomTypeNames = roomTypeRepository
                .findAllById(rooms.stream().map(HotelRoom::getRoomTypeId).distinct().toList()).stream()
                .collect(Collectors.toMap(RoomType::getId, RoomType::getName));

        return rooms.stream()
                .map(room -> toRoomResponse(room, roomTypeNames.get(room.getRoomTypeId())))
                .toList();
    }

    private HotelRoomResponse toRoomResponse(HotelRoom room, String roomTypeName) {
        return new HotelRoomResponse(
                room.getId(),
                room.getRoomTypeId(),
                roomTypeName,
                room.getName(),
                room.getDescription(),
                room.getMaxAdults(),
                room.getMaxChildren(),
                room.getBedCount(),
                room.getBedType(),
                room.getSizeSqm(),
                room.getPricePerNight(),
                room.getCurrencyCode(),
                room.getTotalRooms(),
                room.isActive(),
                room.isAvailable());
    }

    private Hotel findHotelOrThrow(UUID id) {
        return hotelRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found: " + id));
    }

    private City findCityOrThrow(UUID cityId) {
        return cityRepository.findById(cityId)
                .orElseThrow(() -> new ResourceNotFoundException("City not found: " + cityId));
    }

    private RoomType findRoomTypeOrThrow(UUID roomTypeId) {
        return roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Room type not found: " + roomTypeId));
    }

    private HotelImage findImageOrThrow(UUID hotelId, UUID imageId) {
        return hotelImageRepository.findByIdAndHotelId(imageId, hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel image not found: " + imageId));
    }

    private HotelRoom findRoomOrThrow(UUID hotelId, UUID roomId) {
        return hotelRoomRepository.findByIdAndHotelId(roomId, hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel room not found: " + roomId));
    }

}
