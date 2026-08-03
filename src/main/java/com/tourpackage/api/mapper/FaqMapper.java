package com.tourpackage.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.tourpackage.api.dto.response.FaqResponse;
import com.tourpackage.api.entity.Faq;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface FaqMapper {

    FaqResponse toResponse(Faq faq);

}
