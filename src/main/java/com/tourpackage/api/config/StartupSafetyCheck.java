package com.tourpackage.api.config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Refuses to run a production profile with development defaults still in place.
 *
 * <p>The failure this prevents is quiet and severe: the application starts
 * perfectly, serves traffic, and signs its tokens with a secret that is in the
 * repository. Nothing about the running system looks wrong, so nobody finds out
 * until someone else does.
 *
 * <p>Fails hard rather than warning. A warning in a startup log is not read, and
 * the whole point is to make the mistake impossible to deploy past. Outside
 * production the same problems are logged as warnings, because a developer
 * running locally should not be forced to invent secrets.
 */
@Component
public class StartupSafetyCheck {

    private static final Logger log = LoggerFactory.getLogger(StartupSafetyCheck.class);

    /** Must match the default in application.yml. */
    private static final String DEV_JWT_SECRET = "change-this-development-only-secret-key-min-32-chars";

    /** HMAC-SHA256 keys shorter than their 256-bit output add no security. */
    private static final int MIN_JWT_SECRET_BYTES = 32;

    private final Environment environment;
    private final String jwtSecret;
    private final String corsOrigins;
    private final String storagePublicBaseUrl;
    private final boolean rateLimitEnabled;

    public StartupSafetyCheck(
            Environment environment,
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.cors.allowed-origins}") String corsOrigins,
            @Value("${app.storage.public-base-url}") String storagePublicBaseUrl,
            @Value("${app.rate-limit.enabled}") boolean rateLimitEnabled) {
        this.environment = environment;
        this.jwtSecret = jwtSecret;
        this.corsOrigins = corsOrigins;
        this.storagePublicBaseUrl = storagePublicBaseUrl;
        this.rateLimitEnabled = rateLimitEnabled;
    }

    /**
     * Runs once the context is up.
     *
     * <p>Deliberately after startup rather than during bean creation: the report
     * lists every problem at once, which is far more useful than failing on the
     * first one and revealing the next only after it is fixed.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void check() {
        List<String> problems = new ArrayList<>();

        if (DEV_JWT_SECRET.equals(jwtSecret)) {
            problems.add("JWT_SECRET is still the development default — tokens signed with it are forgeable "
                    + "by anyone with access to this repository");
        }
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_JWT_SECRET_BYTES) {
            problems.add("JWT_SECRET is shorter than " + MIN_JWT_SECRET_BYTES + " bytes");
        }
        if (corsOrigins.contains("*")) {
            problems.add("CORS_ALLOWED_ORIGINS contains a wildcard, which allows any site to call this API "
                    + "with a user's credentials");
        }
        if (corsOrigins.contains("localhost")) {
            problems.add("CORS_ALLOWED_ORIGINS still allows localhost");
        }
        if (storagePublicBaseUrl.contains("localhost")) {
            problems.add("STORAGE_PUBLIC_BASE_URL still points at localhost — stored image URLs would be "
                    + "unreachable for every visitor");
        }
        if (!rateLimitEnabled) {
            problems.add("RATE_LIMIT_ENABLED is false, leaving login unprotected against password guessing");
        }

        if (problems.isEmpty()) {
            log.info("Startup safety check passed");
            return;
        }

        String report = String.join("\n  - ", problems);

        if (isProduction()) {
            throw new IllegalStateException(
                    "Refusing to start: development defaults present in a production profile.\n  - " + report);
        }
        log.warn("Startup safety check found {} issue(s) — fine locally, must be fixed before production:\n  - {}",
                problems.size(), report);
    }

    private boolean isProduction() {
        for (String profile : environment.getActiveProfiles()) {
            if (profile.equalsIgnoreCase("prod") || profile.equalsIgnoreCase("production")) {
                return true;
            }
        }
        return false;
    }

}
