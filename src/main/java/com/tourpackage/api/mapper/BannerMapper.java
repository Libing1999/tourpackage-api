package com.tourpackage.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.tourpackage.api.dto.response.BannerResponse;
import com.tourpackage.api.entity.Banner;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface BannerMapper {

    BannerResponse toResponse(Banner banner);

}
