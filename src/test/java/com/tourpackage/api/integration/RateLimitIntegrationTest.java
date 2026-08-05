package com.tourpackage.api.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rate limiting, with the limiter switched back on for this class only.
 *
 * <p>It is off for the rest of the suite because shared counters make unrelated
 * tests fail each other — a class that happens to run after a chatty one would
 * start pre-throttled.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.rate-limit.enabled=true")
class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("repeated login attempts are eventually refused with 429")
    void loginIsRateLimited() throws Exception {
        int rejected = 0;
        int lastStatus = 0;

        // The auth bucket allows 20 a minute; 30 attempts must run into it.
        for (int i = 0; i < 30; i++) {
            lastStatus = mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"attacker@example.com\",\"password\":\"guess-%d\"}".formatted(i)))
                    .andReturn().getResponse().getStatus();
            if (lastStatus == 429) {
                rejected++;
            }
        }

        assertThat(rejected).isPositive();
        assertThat(lastStatus).isEqualTo(429);
    }

    @Test
    @DisplayName("a throttled response tells the caller when to retry")
    void rejectionIncludesRetryAfter() throws Exception {
        String retryAfter = null;
        for (int i = 0; i < 40 && retryAfter == null; i++) {
            var response = mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"attacker2@example.com\",\"password\":\"x\"}"))
                    .andReturn().getResponse();
            if (response.getStatus() == 429) {
                retryAfter = response.getHeader("Retry-After");
            }
        }

        assertThat(retryAfter).isNotNull();
        assertThat(Integer.parseInt(retryAfter)).isPositive();
    }

}
