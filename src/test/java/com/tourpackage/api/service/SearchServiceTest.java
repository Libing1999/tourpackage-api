package com.tourpackage.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.tourpackage.api.dto.response.PopularSearchResponse;
import com.tourpackage.api.repository.SearchRepository;
import com.tourpackage.api.repository.SearchRepository.PopularTermProjection;
import com.tourpackage.api.repository.SearchRepository.SearchHitProjection;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private SearchRepository repository;

    private SearchService service() {
        return new SearchService(repository);
    }

    private PopularTermProjection term(String display, long count) {
        return new PopularTermProjection() {
            public String getTerm() {
                return display;
            }

            public long getCount() {
                return count;
            }
        };
    }

    @Test
    @DisplayName("a one-character query never reaches the database")
    void shortQueryIsNotExecuted() {
        var suggestions = service().suggest("b");

        assertThat(suggestions.total()).isZero();
        verify(repository, never()).suggest(anyString(), anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("LIKE wildcards in a query are escaped so they match literally")
    void escapesLikeWildcards() {
        when(repository.suggest(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(List.of());

        service().suggest("50% off_deal");

        ArgumentCaptor<String> pattern = ArgumentCaptor.forClass(String.class);
        verify(repository, org.mockito.Mockito.atLeastOnce())
                .suggest(anyString(), pattern.capture(), anyString(), anyString(), anyInt());

        // Someone searching "50%" means the two characters, not "50 followed by
        // anything" — which would match nearly every row.
        assertThat(pattern.getValue()).isEqualTo("%50\\% off\\_deal%");
    }

    @Test
    @DisplayName("surrounding and repeated whitespace is normalised away")
    void normalisesWhitespace() {
        when(repository.suggest(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(List.of());

        var suggestions = service().suggest("  bali   beach  ");

        assertThat(suggestions.query()).isEqualTo("bali beach");
    }

    @Test
    @DisplayName("an over-long query is truncated rather than rejected")
    void capsQueryLength() {
        when(repository.suggest(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(List.of());

        var suggestions = service().suggest("x".repeat(500));

        assertThat(suggestions.query()).hasSize(100);
    }

    @Test
    @DisplayName("a search that found nothing is not recorded as a popular term")
    void doesNotRecordZeroResultSearches() {
        Page<SearchHitProjection> empty = new PageImpl<>(List.of(), Pageable.ofSize(12), 0);
        when(repository.search(anyString(), anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(empty);

        service().search("zzzznothing", null, 0, 12);

        // Promoting a term that returns an empty page would recommend a dead end
        // to every future visitor.
        verify(repository, never()).recordSearch(anyString(), anyString());
    }

    @Test
    @DisplayName("a null type becomes the ALL sentinel, never a null bind parameter")
    void nullTypeBecomesSentinel() {
        Page<SearchHitProjection> empty = new PageImpl<>(List.of(), Pageable.ofSize(12), 0);
        when(repository.search(anyString(), anyString(), anyString(), eq("ALL"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(empty);

        service().search("bali", null, 0, 12);

        // PostgreSQL cannot infer the type of a null bind parameter.
        verify(repository).search(anyString(), anyString(), anyString(), eq("ALL"),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("an all-lowercase popular term is capitalised for display")
    void capitalisesLowercaseTerms() {
        when(repository.findPopular(5)).thenReturn(List.of(term("bali", 12), term("beach resort", 7)));

        List<PopularSearchResponse> popular = service().popular(5);

        assertThat(popular).extracting(PopularSearchResponse::term)
                .containsExactly("Bali", "Beach Resort");
    }

    @Test
    @DisplayName("a term with deliberate capitals is left alone")
    void preservesDeliberateCapitals() {
        when(repository.findPopular(5)).thenReturn(List.of(term("USA", 4), term("Rome", 3)));

        assertThat(service().popular(5)).extracting(PopularSearchResponse::term)
                .containsExactly("USA", "Rome");
    }

}
