package com.tourpackage.api.dto.response;

import java.util.List;

/** Everything with a canonical URL of its own, grouped by the route it lives under. */
public record SitemapResponse(
        List<SitemapEntry> hotels,
        List<SitemapEntry> packages,
        List<SitemapEntry> blogPosts) {
}
