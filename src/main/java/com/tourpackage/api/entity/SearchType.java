package com.tourpackage.api.entity;

/**
 * What a search result is.
 *
 * <p>{@code CITY} is what the site calls a "destination" — there is no separate
 * destinations table, a destination is a city with its country and package count.
 */
public enum SearchType {
    HOTEL,
    PACKAGE,
    CITY,
    COUNTRY
}
