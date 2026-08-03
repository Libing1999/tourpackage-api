package com.tourpackage.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import com.tourpackage.api.dto.response.RoomTypeResponse;
import com.tourpackage.api.entity.RoomType;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RoomTypeMapper {

    @Mapping(target = "isActive", source = "active")
    RoomTypeResponse toResponse(RoomType roomType);

}
