package com.tourpackage.api.dto.response;

import java.util.List;
import java.util.Map;

/**
 * The whole editorial layer of the site in one response: every content block
 * keyed by its dotted key, plus the header and footer links.
 *
 * <p>One call because the navbar and footer are on every page — fetching them
 * separately would mean three requests before anything renders, and they change
 * rarely enough to cache together.
 */
public record SiteContentResponse(
        Map<String, ContentBlockResponse> blocks,
        List<NavLinkResponse> header,
        List<NavLinkResponse> footer
) {
}
