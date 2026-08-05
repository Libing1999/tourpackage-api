package com.tourpackage.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tourpackage.api.dto.response.ApiResponse;
import com.tourpackage.api.dto.response.PageResponse;
import com.tourpackage.api.dto.response.PopularSearchResponse;
import com.tourpackage.api.dto.response.SearchHit;
import com.tourpackage.api.dto.response.SearchSuggestions;
import com.tourpackage.api.entity.SearchType;
import com.tourpackage.api.service.SearchService;

/**
 * Global search over hotels, packages, destinations and countries.
 *
 * <p>Two read endpoints with very different shapes on purpose. {@code /suggest}
 * is called on almost every keystroke and returns a handful of grouped hits;
 * {@code /search} is called once when a search is submitted, is paginated, and
 * is the only one that counts towards popular searches.
 */
@RestController
@RequestMapping("/public/search")
public class SearchController {

    private static final int MAX_PAGE_SIZE = 50;

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /** Autocomplete. Returns empty groups rather than an error for a short query. */
    @GetMapping("/suggest")
    public ApiResponse<SearchSuggestions> suggest(@RequestParam(name = "q", defaultValue = "") String query) {
        return ApiResponse.of(searchService.suggest(query));
    }

    /**
     * Full results.
     *
     * @param type restricts to one kind of result; omitted means all of them
     */
    @GetMapping
    public ApiResponse<PageResponse<SearchHit>> search(
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(required = false) SearchType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ApiResponse.of(searchService.search(
                query, type, Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE)));
    }

    /** What other people search for — shown before anything has been typed. */
    @GetMapping("/popular")
    public ApiResponse<List<PopularSearchResponse>> popular(
            @RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.of(searchService.popular(Math.clamp(limit, 1, 20)));
    }

}
