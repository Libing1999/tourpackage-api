package com.tourpackage.api.dto.response;

import com.tourpackage.api.entity.SearchType;

/**
 * One result, in the shape the UI renders regardless of what was matched.
 *
 * <p>A hotel, a package, a city and a country have almost nothing in common as
 * records, but a search result list has to show them side by side. Flattening
 * them here rather than returning four typed lists means the frontend has one
 * card component and the ranking can interleave types by relevance.
 *
 * @param subtitle context that disambiguates same-named results — the country
 *                 for a city, the city for a hotel
 * @param url      path on the public site, built server-side because only the
 *                 backend knows which slug belongs to which route
 */
public record SearchHit(
        SearchType type,
        String id,
        String title,
        String subtitle,
        String imageUrl,
        String url) {
}
