package com.tourpackage.api.dto.response;

/** A frequently-used search term, shown before anyone has typed anything. */
public record PopularSearchResponse(String term, long searchCount) {
}
