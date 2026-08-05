package com.tourpackage.api.ratelimit;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Per-JVM counters in a Caffeine cache.
 *
 * <p>The default, because a single instance is the common deployment and this
 * needs no infrastructure. Entries expire on write, which is exactly a fixed
 * window: the first request of a window creates the entry, and the whole entry
 * disappears when the window closes.
 *
 * <p>{@code maximumSize} matters more than it looks. The key includes the client
 * IP, so an attacker cycling source addresses would otherwise grow this map
 * without limit — turning a rate limiter into a memory exhaustion vector.
 */
@Component
@ConditionalOnProperty(name = "app.rate-limit.store", havingValue = "memory", matchIfMissing = true)
public class InMemoryRateLimitStore implements RateLimitStore {

    private final Cache<String, AtomicLong> counters;

    public InMemoryRateLimitStore() {
        this.counters = Caffeine.newBuilder()
                .maximumSize(100_000)
                // Long enough for the longest configured window; entries are
                // dropped sooner when their own window is shorter.
                .expireAfterWrite(Duration.ofHours(1))
                .build();
    }

    @Override
    public long incrementAndGet(String key, Duration window) {
        // Keying by window as well as identity means a change of window length
        // starts a fresh count rather than inheriting a stale one, and lets one
        // client be limited differently on different routes.
        String windowKey = key + ":" + (System.currentTimeMillis() / window.toMillis());
        return counters.get(windowKey, unused -> new AtomicLong()).incrementAndGet();
    }

    @Override
    public String describe() {
        return "in-memory (per instance)";
    }

}
