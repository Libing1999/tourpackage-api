package com.tourpackage.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import com.tourpackage.api.dto.response.AmenityResponse;
import com.tourpackage.api.entity.Amenity;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AmenityMapper {

    @Mapping(target = "isActive", source = "active")
    AmenityResponse toResponse(Amenity amenity);

}
