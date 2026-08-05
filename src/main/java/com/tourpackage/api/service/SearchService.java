package com.tourpackage.api.service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tourpackage.api.config.CacheConfig;
import com.tourpackage.api.dto.response.PageResponse;
import com.tourpackage.api.dto.response.PopularSearchResponse;
import com.tourpackage.api.dto.response.SearchHit;
import com.tourpackage.api.dto.response.SearchSuggestions;
import com.tourpackage.api.entity.SearchType;
import com.tourpackage.api.repository.SearchRepository;
import com.tourpackage.api.repository.SearchRepository.SearchHitProjection;

@Service
@Transactional(readOnly = true)
public class SearchService {

    /** Below this, a query matches so much that the results are meaningless. */
    private static final int MIN_QUERY_LENGTH = 2;

    /** Matches the {@code term} column; longer input is a paste, not a search. */
    private static final int MAX_QUERY_LENGTH = 100;

    private static final int MAX_SUGGESTIONS_PER_TYPE = 5;

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /** {@code %} and {@code _} are wildcards in LIKE; a visitor typing them means them literally. */
    private static final Pattern LIKE_WILDCARDS = Pattern.compile("([%_\\\\])");

    private final SearchRepository searchRepository;

    public SearchService(SearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    /**
     * Autocomplete: a few hits of each type.
     *
     * <p>Runs one query per type rather than one combined query, so that a type
     * with many matches cannot crowd the others out of the dropdown. Four
     * indexed lookups of five rows each is not the expensive part of this
     * request.
     */
    public SearchSuggestions suggest(String rawQuery) {
        String query = normalize(rawQuery);
        if (query.length() < MIN_QUERY_LENGTH) {
            return new SearchSuggestions(query, List.of(), List.of(), List.of(), List.of(), 0);
        }

        List<SearchHit> hotels = suggestOne(query, SearchType.HOTEL);
        List<SearchHit> packages = suggestOne(query, SearchType.PACKAGE);
        List<SearchHit> destinations = suggestOne(query, SearchType.CITY);
        List<SearchHit> countries = suggestOne(query, SearchType.COUNTRY);

        return new SearchSuggestions(
                query, hotels, packages, destinations, countries,
                hotels.size() + packages.size() + destinations.size() + countries.size());
    }

    /**
     * A page of results, and the record that someone searched for this.
     *
     * <p>Tracking happens here and not in {@link #suggest} on purpose. Autocomplete
     * fires as someone types, so counting it would fill the popular list with
     * "b", "ba", "bal" — every prefix of every real search. A submitted search is
     * the only signal that a whole term was meant.
     */
    @Transactional
    public PageResponse<SearchHit> search(String rawQuery, SearchType type, int page, int size) {
        String query = normalize(rawQuery);
        if (query.length() < MIN_QUERY_LENGTH) {
            return PageResponse.of(Page.empty(PageRequest.of(page, size)));
        }

        Page<SearchHitProjection> results = searchRepository.search(
                query,
                containsPattern(query),
                prefixPattern(query),
                type == null ? "ALL" : type.name(),
                PageRequest.of(page, size));

        // Only terms that found something are recorded. "Popular searches" is a
        // set of suggestions, and suggesting a search that returns an empty page
        // is worse than suggesting nothing — a typo searched twice would
        // otherwise be promoted into the list for everyone.
        if (results.getTotalElements() > 0) {
            recordTerm(query);
        }

        return PageResponse.of(results.map(this::toHit));
    }

    @Cacheable(value = CacheConfig.POPULAR_SEARCHES, key = "#limit")
    public List<PopularSearchResponse> popular(int limit) {
        return searchRepository.findPopular(limit).stream()
                .map(row -> new PopularSearchResponse(present(row.getTerm()), row.getCount()))
                .toList();
    }

    /**
     * Tidies a stored term for display.
     *
     * <p>{@code display_term} is whatever was last typed, and most people type
     * in lowercase — left alone, the popular list drifts to "bali", "dubai",
     * "paris" and reads like a bug. Capitalising only when the term is entirely
     * lowercase leaves anything with deliberate capitals ("USA", "Rome") alone,
     * rather than title-casing it into "Usa".
     */
    private String present(String term) {
        if (!term.equals(term.toLowerCase(Locale.ROOT))) {
            return term;
        }
        return Arrays.stream(term.split(" "))
                .map(word -> word.isEmpty() ? word
                        : Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    private List<SearchHit> suggestOne(String query, SearchType type) {
        return searchRepository
                .suggest(query, containsPattern(query), prefixPattern(query), type.name(), MAX_SUGGESTIONS_PER_TYPE)
                .stream()
                .map(this::toHit)
                .toList();
    }

    /**
     * A term is stored lowercased so spellings collapse into one row, alongside
     * the spelling actually typed so the popular list doesn't read as all-lowercase.
     */
    private void recordTerm(String query) {
        searchRepository.recordSearch(query.toLowerCase(Locale.ROOT), query);
    }

    private SearchHit toHit(SearchHitProjection row) {
        SearchType type = SearchType.valueOf(row.getType());
        return new SearchHit(
                type,
                row.getId(),
                row.getTitle(),
                row.getSubtitle(),
                row.getImage(),
                url(type, row));
    }

    /**
     * Where a result links to.
     *
     * <p>Built here rather than in the client because only the backend knows
     * which identifier a route wants: hotels and packages are addressed by slug,
     * while a city or country is a filter on the packages listing and is
     * addressed by id.
     */
    private String url(SearchType type, SearchHitProjection row) {
        return switch (type) {
            case HOTEL -> "/hotels/" + row.getSlug();
            case PACKAGE -> "/packages/" + row.getSlug();
            case CITY -> "/packages?city=" + row.getId();
            case COUNTRY -> "/packages?country=" + row.getId();
        };
    }

    /** Collapses whitespace and trims; length is capped rather than rejected. */
    private String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String collapsed = WHITESPACE.matcher(raw).replaceAll(" ").strip();
        return collapsed.length() > MAX_QUERY_LENGTH ? collapsed.substring(0, MAX_QUERY_LENGTH) : collapsed;
    }

    private String containsPattern(String query) {
        return "%" + escapeLike(query) + "%";
    }

    private String prefixPattern(String query) {
        return escapeLike(query) + "%";
    }

    /**
     * Escapes LIKE metacharacters.
     *
     * <p>Not a SQL-injection guard — the value is a bound parameter, so it can
     * never be SQL. This is about meaning: searching for "50%" should look for
     * the two characters, not "50 followed by anything".
     */
    private String escapeLike(String value) {
        return LIKE_WILDCARDS.matcher(value).replaceAll("\\\\$1");
    }

}
