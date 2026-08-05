package com.tourpackage.api.dto.response;

import java.time.Instant;

/**
 * One indexable URL's slug and when it last changed.
 *
 * <p>Deliberately just these two fields. A sitemap needs a location and a
 * lastmod and nothing else, and this endpoint is fetched for every entity on
 * the site at once — pulling full listing DTOs with their images and joins to
 * build an XML file of URLs would be several orders of magnitude more data than
 * the file itself.
 */
public record SitemapEntry(String slug, Instant updatedAt) {
}
