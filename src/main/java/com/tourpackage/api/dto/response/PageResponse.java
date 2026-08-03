package com.tourpackage.api.dto.response;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Mirrors the frontend's {@code PaginatedResponse<T>} type field-for-field
 * (see tourpackage-web/src/types/api.ts) — that type was scaffolded before
 * any endpoint actually used it; this is the first one that does.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }

}
