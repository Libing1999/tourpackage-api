package com.tourpackage.api.dto.response;

import java.util.UUID;

public record SettingResponse(
        UUID id,
        String key,
        String value,
        String valueType,
        String groupName,
        boolean isPublic
) {
}
