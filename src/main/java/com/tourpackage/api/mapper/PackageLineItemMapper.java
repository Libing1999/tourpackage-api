package com.tourpackage.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.tourpackage.api.dto.response.PackageLineItemResponse;
import com.tourpackage.api.entity.PackageExclude;
import com.tourpackage.api.entity.PackageInclude;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PackageLineItemMapper {

    PackageLineItemResponse toResponse(PackageInclude include);

    PackageLineItemResponse toResponse(PackageExclude exclude);

}
