package com.tourpackage.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import com.tourpackage.api.dto.response.HotelImageResponse;
import com.tourpackage.api.entity.HotelImage;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface HotelImageMapper {

    @Mapping(target = "isCover", source = "cover")
    HotelImageResponse toResponse(HotelImage image);

}
