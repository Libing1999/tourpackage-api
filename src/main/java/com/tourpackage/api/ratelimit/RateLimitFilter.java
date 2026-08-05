package com.tourpackage.api.ratelimit;

import java.io.IOException;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Fixed-window rate limiting, with a different budget per kind of endpoint.
 *
 * <p>Runs before authentication, because the endpoints most worth protecting are
 * the ones an unauthenticated caller can reach: login is a password-guessing
 * target, and the contact and newsletter forms are spam targets. A limiter that
 * only applied after authentication would protect nothing that needs it.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    /**
     * How requests are grouped, and why the budgets differ so much.
     *
     * <p>{@code AUTH} is the tightest: password guessing is the attack, and no
     * legitimate person signs in twenty times a minute. {@code WRITE} covers the
     * anonymous forms — enough for a real person who mistypes an email twice,
     * far too little to be a useful spam channel. {@code READ} exists mostly to
     * blunt scrapers; the number is high enough that browsing the site normally,
     * including a burst of autocomplete requests, never approaches it.
     */
    private enum Bucket {
        AUTH(20, Duration.ofMinutes(1)),
        WRITE(10, Duration.ofMinutes(1)),
        READ(300, Duration.ofMinutes(1));

        final int limit;
        final Duration window;

        Bucket(int limit, Duration window) {
            this.limit = limit;
            this.window = window;
        }
    }

    private final RateLimitStore store;
    private final boolean enabled;
    private final boolean trustForwardedFor;

    public RateLimitFilter(
            RateLimitStore store,
            @Value("${app.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.rate-limit.trust-forwarded-for:false}") boolean trustForwardedFor) {
        this.store = store;
        this.enabled = enabled;
        this.trustForwardedFor = trustForwardedFor;
    }

    /** Records which mode is live, so a misconfigured fleet is visible at boot. */
    @jakarta.annotation.PostConstruct
    void logConfiguration() {
        log.info("Rate limiting {} using {}", enabled ? "enabled" : "DISABLED", store.describe());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        String path = pathWithinApplication(request);
        // Probes and metrics are scraped on a schedule and must never be
        // throttled — a rate-limited health check reads as an outage.
        return path.startsWith("/actuator") || path.startsWith("/uploads");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Bucket bucket = bucketFor(request);
        String key = bucket.name() + ":" + clientIp(request);

        long used = store.incrementAndGet(key, bucket.window);
        long remaining = Math.max(0, bucket.limit - used);

        response.setHeader("X-RateLimit-Limit", String.valueOf(bucket.limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

        if (used > bucket.limit) {
            // Logged at warn: a client hitting a limit is either broken or
            // hostile, and both are worth seeing.
            log.warn("Rate limit exceeded: {} {} from {} ({} in {})",
                    request.getMethod(), request.getRequestURI(), clientIp(request), used, bucket.window);
            reject(response, bucket);
            return;
        }

        chain.doFilter(request, response);
    }

    private Bucket bucketFor(HttpServletRequest request) {
        String path = pathWithinApplication(request);

        if (path.startsWith("/auth/")) {
            return Bucket.AUTH;
        }
        // Anonymous writes: bookings, enquiries, newsletter signups.
        if (!"GET".equals(request.getMethod()) && !"HEAD".equals(request.getMethod())) {
            return Bucket.WRITE;
        }
        return Bucket.READ;
    }

    /**
     * The request path with the servlet context stripped.
     *
     * <p>{@code getRequestURI()} includes the context path, so with the API
     * mounted at {@code /api} a login arrives as {@code /api/auth/login} and a
     * {@code startsWith("/auth/")} test silently never matches. That is not a
     * theoretical concern: it put login in the general write bucket instead of
     * the auth one, quietly applying the wrong limit. Spring Security's own
     * matchers work on this context-relative form, so doing the same here keeps
     * the two agreeing about what a path is.
     */
    private String pathWithinApplication(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        return context != null && !context.isEmpty() && uri.startsWith(context)
                ? uri.substring(context.length())
                : uri;
    }

    /**
     * The caller's address.
     *
     * <p>{@code X-Forwarded-For} is only believed when explicitly configured.
     * The header is client-settable, so trusting it by default would let anyone
     * bypass every limit here by sending a different value each request — and
     * would let them exhaust the store's key space while doing it. Enable it
     * only when a proxy that overwrites the header sits in front.
     */
    private String clientIp(HttpServletRequest request) {
        if (trustForwardedFor) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                // Left-most entry is the original client; the rest are proxies.
                return forwarded.split(",")[0].strip();
            }
        }
        return request.getRemoteAddr();
    }

    private void reject(HttpServletResponse response, Bucket bucket) throws IOException {
        long retryAfterSeconds = bucket.window.toSeconds();

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        // Written directly rather than thrown: the exception handlers live in
        // the MVC layer, and this filter runs before the request reaches it.
        response.getWriter().write("""
                {"success":false,"message":"Too many requests. Please try again shortly.","data":null}""");
    }

}
