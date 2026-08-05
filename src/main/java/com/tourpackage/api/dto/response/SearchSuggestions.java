package com.tourpackage.api.dto.response;

import java.util.List;

/**
 * Autocomplete payload: a few hits per type rather than one ranked list.
 *
 * <p>Grouping is a deliberate product choice. A single ranked list of ten can
 * legitimately be ten hotels, which hides from the visitor that the site also
 * has packages and destinations matching what they typed.
 */
public record SearchSuggestions(
        String query,
        List<SearchHit> hotels,
        List<SearchHit> packages,
        List<SearchHit> destinations,
        List<SearchHit> countries,
        int total) {
}
