package com.tourpackage.api.integration;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The public API as an anonymous visitor sees it.
 *
 * <p>Runs the whole stack — filters, security, controllers, services, and a real
 * PostgreSQL built by the production migrations. The point is to catch what unit
 * tests structurally cannot: the security chain letting the wrong thing through,
 * a query that only fails against real SQL, a filter ordering mistake.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("seeded content is served to anonymous callers")
    void publicContentIsAnonymous() throws Exception {
        mockMvc.perform(get("/public/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.site_name").value(notNullValue()));

        mockMvc.perform(get("/public/hotels").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements", greaterThan(0)));
    }

    @Test
    @DisplayName("pagination honours the requested page size")
    void paginationIsHonoured() throws Exception {
        mockMvc.perform(get("/public/tour-packages").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.page").value(0));
    }

    @Test
    @DisplayName("global search finds a seeded hotel, package and city together")
    void searchSpansEveryType() throws Exception {
        mockMvc.perform(get("/public/search/suggest").param("q", "bali"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total", greaterThan(0)));
    }

    @Test
    @DisplayName("search tolerates a typo, which needs the real trigram operators")
    void searchIsTypoTolerant() throws Exception {
        // Only meaningful against PostgreSQL: this is the `<%` word-similarity
        // operator, which no in-memory database implements.
        mockMvc.perform(get("/public/search/suggest").param("q", "serenty"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total", greaterThan(0)));
    }

    @Test
    @DisplayName("every response carries a correlation id")
    void responsesAreTraceable() throws Exception {
        mockMvc.perform(get("/public/settings"))
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    @DisplayName("an inbound correlation id is echoed rather than replaced")
    void inboundCorrelationIdSurvives() throws Exception {
        mockMvc.perform(get("/public/settings").header("X-Request-Id", "trace-abc-123"))
                .andExpect(header().string("X-Request-Id", "trace-abc-123"));
    }

    @Test
    @DisplayName("a forged correlation id is stripped of anything but safe characters")
    void correlationIdIsSanitised() throws Exception {
        mockMvc.perform(get("/public/settings").header("X-Request-Id", "bad\nvalue<script>"))
                .andExpect(header().string("X-Request-Id", not(org.hamcrest.Matchers.containsString("<"))));
    }

    @Test
    @DisplayName("security headers are present on public responses")
    void securityHeadersArePresent() throws Exception {
        mockMvc.perform(get("/public/settings"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
    }

    @Test
    @DisplayName("an invalid newsletter address is a 400 with the offending field named")
    void validationReportsFieldErrors() throws Exception {
        mockMvc.perform(post("/public/newsletter/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    @DisplayName("an unknown path is 404, not 500")
    void unknownPathIsNotFound() throws Exception {
        mockMvc.perform(get("/public/does-not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("the wrong verb is 405, not 500")
    void wrongMethodIsMethodNotAllowed() throws Exception {
        mockMvc.perform(get("/public/newsletter/subscribe"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("the wrong content type is 415, not 500")
    void wrongContentTypeIsUnsupportedMedia() throws Exception {
        mockMvc.perform(post("/public/newsletter/subscribe")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("a non-numeric page parameter is 400 with a field error")
    void badParameterTypeIsBadRequest() throws Exception {
        mockMvc.perform(get("/public/search").param("q", "bali").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.page").exists());
    }

    @Test
    @DisplayName("an unknown enum value is rejected without leaking Java types")
    void invalidEnumIsRejectedCleanly() throws Exception {
        mockMvc.perform(post("/public/inquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"A","email":"a@b.co","phone":"1","message":"hi","partySize":"lots"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(not(org.hamcrest.Matchers.containsString("java."))));
    }

}
