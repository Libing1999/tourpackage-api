package com.tourpackage.api.dto.response;

import java.util.UUID;

public record PageSeoResponse(
        UUID id,
        String path,
        String metaTitle,
        String metaDescription,
        String ogImageUrl,
        boolean noIndex
) {
}
