package com.tourpackage.api.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tourpackage.api.dto.request.AmenityRequest;
import com.tourpackage.api.dto.response.AmenityResponse;
import com.tourpackage.api.entity.Amenity;
import com.tourpackage.api.exception.DuplicateResourceException;
import com.tourpackage.api.exception.ResourceNotFoundException;
import com.tourpackage.api.mapper.AmenityMapper;
import com.tourpackage.api.repository.AmenityRepository;

@Service
@Transactional
public class AmenityService {

    private final AmenityRepository amenityRepository;
    private final AmenityMapper amenityMapper;

    public AmenityService(AmenityRepository amenityRepository, AmenityMapper amenityMapper) {
        this.amenityRepository = amenityRepository;
        this.amenityMapper = amenityMapper;
    }

    @Transactional(readOnly = true)
    public List<AmenityResponse> list() {
        return amenityRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .map(amenityMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AmenityResponse> listActive() {
        return amenityRepository.findAllByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                .map(amenityMapper::toResponse)
                .toList();
    }

    public AmenityResponse create(AmenityRequest request) {
        if (amenityRepository.existsBySlug(request.slug())) {
            throw new DuplicateResourceException("An amenity with slug '" + request.slug() + "' already exists");
        }

        Amenity amenity = Amenity.builder()
                .name(request.name())
                .slug(request.slug())
                .icon(request.icon())
                .category(request.category())
                .displayOrder(request.displayOrder())
                .active(request.isActive())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return amenityMapper.toResponse(amenityRepository.save(amenity));
    }

    public AmenityResponse update(UUID id, AmenityRequest request) {
        Amenity amenity = findOrThrow(id);

        if (amenityRepository.existsBySlugAndIdNot(request.slug(), id)) {
            throw new DuplicateResourceException("An amenity with slug '" + request.slug() + "' already exists");
        }

        amenity.setName(request.name());
        amenity.setSlug(request.slug());
        amenity.setIcon(request.icon());
        amenity.setCategory(request.category());
        amenity.setDisplayOrder(request.displayOrder());
        amenity.setActive(request.isActive());

        return amenityMapper.toResponse(amenityRepository.save(amenity));
    }

    public void delete(UUID id) {
        Amenity amenity = findOrThrow(id);
        amenityRepository.delete(amenity);
    }

    private Amenity findOrThrow(UUID id) {
        return amenityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Amenity not found: " + id));
    }

}
