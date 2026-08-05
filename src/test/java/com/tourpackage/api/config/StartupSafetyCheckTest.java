package com.tourpackage.api.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class StartupSafetyCheckTest {

    private static final String DEV_SECRET = "change-this-development-only-secret-key-min-32-chars";
    private static final String GOOD_SECRET = "a-real-secret-that-is-definitely-long-enough-32";

    private StartupSafetyCheck check(String profile, String secret, String cors, String storageUrl,
            boolean rateLimit) {
        MockEnvironment env = new MockEnvironment();
        if (profile != null) {
            env.setActiveProfiles(profile);
        }
        return new StartupSafetyCheck(env, secret, cors, storageUrl, rateLimit);
    }

    @Test
    @DisplayName("production refuses to start with the development JWT secret")
    void productionRejectsDevSecret() {
        assertThatThrownBy(() -> check("prod", DEV_SECRET, "https://x.com", "https://x.com", true).check())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    @DisplayName("production refuses a wildcard CORS origin")
    void productionRejectsWildcardCors() {
        assertThatThrownBy(() -> check("prod", GOOD_SECRET, "*", "https://x.com", true).check())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("wildcard");
    }

    @Test
    @DisplayName("production refuses a short JWT secret")
    void productionRejectsShortSecret() {
        assertThatThrownBy(() -> check("prod", "too-short", "https://x.com", "https://x.com", true).check())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("shorter than");
    }

    @Test
    @DisplayName("production refuses to run with rate limiting switched off")
    void productionRejectsDisabledRateLimit() {
        assertThatThrownBy(() -> check("prod", GOOD_SECRET, "https://x.com", "https://x.com", false).check())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RATE_LIMIT_ENABLED");
    }

    @Test
    @DisplayName("every problem is reported at once, not one per restart")
    void reportsAllProblemsTogether() {
        assertThatThrownBy(() -> check("prod", DEV_SECRET, "*", "http://localhost/x", false).check())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET")
                .hasMessageContaining("wildcard")
                .hasMessageContaining("localhost")
                .hasMessageContaining("RATE_LIMIT_ENABLED");
    }

    @Test
    @DisplayName("a correct production configuration starts")
    void productionAcceptsGoodConfiguration() {
        assertThatCode(() -> check("prod", GOOD_SECRET, "https://tourpackage.com",
                "https://cdn.tourpackage.com/uploads", true).check())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("outside production the same problems only warn")
    void developmentOnlyWarns() {
        // A developer must not be forced to invent secrets to run the app.
        assertThatCode(() -> check(null, DEV_SECRET, "http://localhost:3000",
                "http://localhost:8080/api/uploads", true).check())
                .doesNotThrowAnyException();
    }

}
