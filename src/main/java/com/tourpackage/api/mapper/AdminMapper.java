package com.tourpackage.api.mapper;

import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Mapper;

import com.tourpackage.api.dto.response.AdminProfileResponse;
import com.tourpackage.api.entity.Admin;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AdminMapper {

    @Mapping(target = "emailVerified", expression = "java(admin.isEmailVerified())")
    AdminProfileResponse toProfileResponse(Admin admin);

}
