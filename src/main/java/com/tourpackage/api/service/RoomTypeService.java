package com.tourpackage.api.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tourpackage.api.dto.request.RoomTypeRequest;
import com.tourpackage.api.dto.response.RoomTypeResponse;
import com.tourpackage.api.entity.RoomType;
import com.tourpackage.api.exception.ApiException;
import com.tourpackage.api.exception.DuplicateResourceException;
import com.tourpackage.api.exception.ResourceNotFoundException;
import com.tourpackage.api.mapper.RoomTypeMapper;
import com.tourpackage.api.repository.HotelRoomRepository;
import com.tourpackage.api.repository.RoomTypeRepository;

import org.springframework.http.HttpStatus;

@Service
@Transactional
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;
    private final HotelRoomRepository hotelRoomRepository;
    private final RoomTypeMapper roomTypeMapper;

    public RoomTypeService(
            RoomTypeRepository roomTypeRepository,
            HotelRoomRepository hotelRoomRepository,
            RoomTypeMapper roomTypeMapper) {
        this.roomTypeRepository = roomTypeRepository;
        this.hotelRoomRepository = hotelRoomRepository;
        this.roomTypeMapper = roomTypeMapper;
    }

    @Transactional(readOnly = true)
    public List<RoomTypeResponse> list() {
        return roomTypeRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .map(roomTypeMapper::toResponse)
                .toList();
    }

    public RoomTypeResponse create(RoomTypeRequest request) {
        if (roomTypeRepository.existsBySlug(request.slug())) {
            throw new DuplicateResourceException("A room type with slug '" + request.slug() + "' already exists");
        }

        RoomType roomType = RoomType.builder()
                .name(request.name())
                .slug(request.slug())
                .description(request.description())
                .maxOccupancy(request.maxOccupancy())
                .displayOrder(request.displayOrder())
                .active(request.isActive())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return roomTypeMapper.toResponse(roomTypeRepository.save(roomType));
    }

    public RoomTypeResponse update(UUID id, RoomTypeRequest request) {
        RoomType roomType = findOrThrow(id);

        if (roomTypeRepository.existsBySlugAndIdNot(request.slug(), id)) {
            throw new DuplicateResourceException("A room type with slug '" + request.slug() + "' already exists");
        }

        roomType.setName(request.name());
        roomType.setSlug(request.slug());
        roomType.setDescription(request.description());
        roomType.setMaxOccupancy(request.maxOccupancy());
        roomType.setDisplayOrder(request.displayOrder());
        roomType.setActive(request.isActive());

        return roomTypeMapper.toResponse(roomTypeRepository.save(roomType));
    }

    public void delete(UUID id) {
        RoomType roomType = findOrThrow(id);

        if (hotelRoomRepository.existsByRoomTypeId(id)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Cannot delete room type '" + roomType.getName() + "' — it is still used by one or more hotel rooms");
        }

        roomTypeRepository.delete(roomType);
    }

    private RoomType findOrThrow(UUID id) {
        return roomTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room type not found: " + id));
    }

}
