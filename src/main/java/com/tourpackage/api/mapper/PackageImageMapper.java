package com.tourpackage.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import com.tourpackage.api.dto.response.PackageImageResponse;
import com.tourpackage.api.entity.PackageImage;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PackageImageMapper {

    @Mapping(target = "isCover", source = "cover")
    PackageImageResponse toResponse(PackageImage image);

}
