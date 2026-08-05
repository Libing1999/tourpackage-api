package com.tourpackage.api.ratelimit;

import java.time.Duration;

/**
 * Counts hits per key within a window.
 *
 * <p>The abstraction exists so that rate limiting survives horizontal scaling.
 * The in-memory implementation counts per JVM, which means N instances behind a
 * load balancer allow N times the configured limit — fine for one instance,
 * wrong for a fleet. Swapping in {@link RedisRateLimitStore} makes the counter
 * shared, and that is a configuration change rather than a code change.
 *
 * <p>Implementations must be safe under concurrency: the increment and the
 * expiry have to be one atomic step, or two simultaneous first-requests can
 * leave a key with no expiry set and a counter that never resets.
 */
public interface RateLimitStore {

    /**
     * Increments the counter for {@code key} and returns its new value, setting
     * the window expiry on first use.
     *
     * @return hits recorded in the current window, including this one
     */
    long incrementAndGet(String key, Duration window);

    /** For the health endpoint and for logging which mode is live. */
    String describe();

}
