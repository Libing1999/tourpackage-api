package com.tourpackage.api.ratelimit;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Counters shared across every instance.
 *
 * <p>Activated with {@code APP_RATE_LIMIT_STORE=redis}. Nothing else changes:
 * {@link RateLimitFilter} only ever sees {@link RateLimitStore}.
 *
 * <p>{@code INCR} creates the key at 1 if absent and is atomic, so concurrent
 * first-requests cannot both think they are first. The expiry is set only when
 * the counter comes back as 1 — setting it on every request would slide the
 * window forward continuously and a client sending steady traffic would never
 * reset.
 */
@Component
@ConditionalOnProperty(name = "app.rate-limit.store", havingValue = "redis")
public class RedisRateLimitStore implements RateLimitStore {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimitStore.class);

    private static final String PREFIX = "ratelimit:";

    private final StringRedisTemplate redis;

    public RedisRateLimitStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public long incrementAndGet(String key, Duration window) {
        String redisKey = PREFIX + key + ":" + (System.currentTimeMillis() / window.toMillis());

        try {
            Long count = redis.opsForValue().increment(redisKey);

            if (count != null && count == 1L) {
                redis.expire(redisKey, window);
            }
            return count == null ? 1L : count;
        } catch (RuntimeException ex) {
            // Fails open, and this branch is load-bearing rather than defensive
            // decoration. Without it a connection failure propagates out of the
            // filter, which runs before the MVC exception handlers — the whole
            // API then answers 401, because the security entry point is what
            // ends up handling it. An unreachable rate-limit store must degrade
            // the limiter, not take the site down.
            log.warn("Rate limit store unavailable, allowing request: {}", ex.getMessage());
            return 1L;
        }
    }

    @Override
    public String describe() {
        return "redis (shared across instances)";
    }

}
