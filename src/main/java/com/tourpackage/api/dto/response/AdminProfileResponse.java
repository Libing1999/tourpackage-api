package com.tourpackage.api.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.tourpackage.api.entity.AdminRole;

public record AdminProfileResponse(
        UUID id,
        String fullName,
        String email,
        AdminRole role,
        String phone,
        String avatarUrl,
        boolean emailVerified,
        Instant lastLoginAt,
        Instant createdAt
) {
}
