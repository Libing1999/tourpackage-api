package com.tourpackage.api.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * Uses a hand-written stub rather than Mockito: {@code StringRedisTemplate}
 * cannot be mocked by the default mock maker, and a stub is clearer here than
 * adding a byte-buddy agent to the build for four tests.
 */
class RedisRateLimitStoreTest {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    /** Records the expiries that were set, and returns a scripted INCR reply. */
    private static class StubRedis extends StringRedisTemplate {
        private final Supplier<Long> incrementReply;
        final List<String> expiriesSet = new ArrayList<>();

        StubRedis(Supplier<Long> incrementReply) {
            this.incrementReply = incrementReply;
        }

        @Override
        public ValueOperations<String, String> opsForValue() {
            return valueOpsReturning(incrementReply);
        }

        @Override
        public Boolean expire(String key, Duration timeout) {
            expiriesSet.add(key);
            return true;
        }
    }

    /**
     * A proxy over {@code ValueOperations} answering only {@code increment}.
     *
     * <p>The interface has dozens of methods and the store calls exactly one, so
     * a proxy says what a hand-written implementation would bury.
     */
    @SuppressWarnings("unchecked")
    private static ValueOperations<String, String> valueOpsReturning(Supplier<Long> incrementReply) {
        return (ValueOperations<String, String>) java.lang.reflect.Proxy.newProxyInstance(
                ValueOperations.class.getClassLoader(),
                new Class<?>[] { ValueOperations.class },
                (proxy, method, args) -> {
                    if ("increment".equals(method.getName()) && args != null && args.length == 1) {
                        return incrementReply.get();
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    @Test
    @DisplayName("returns the counter Redis reports")
    void returnsCount() {
        assertThat(new RedisRateLimitStore(new StubRedis(() -> 7L)).incrementAndGet("k", WINDOW))
                .isEqualTo(7L);
    }

    @Test
    @DisplayName("sets the expiry on the first request of a window, and not after")
    void setsExpiryOnlyOnFirstHit() {
        StubRedis firstHit = new StubRedis(() -> 1L);
        new RedisRateLimitStore(firstHit).incrementAndGet("k", WINDOW);
        assertThat(firstHit.expiriesSet).hasSize(1);

        // Re-setting the expiry every hit would slide the window forward
        // continuously, so steady traffic would never reset.
        StubRedis laterHit = new StubRedis(() -> 5L);
        new RedisRateLimitStore(laterHit).incrementAndGet("k", WINDOW);
        assertThat(laterHit.expiriesSet).isEmpty();
    }

    @Test
    @DisplayName("an unreachable Redis allows the request rather than failing it")
    void failsOpenWhenRedisIsDown() {
        StubRedis down = new StubRedis(() -> {
            throw new RedisConnectionFailureException("Unable to connect to Redis");
        });

        // Regression: this exception propagated out of the filter, which runs
        // before the MVC exception handlers, so the security entry point ended
        // up answering — and every public endpoint returned 401 while Redis was
        // down. Verified against a real dead port before this test existed.
        assertThat(new RedisRateLimitStore(down).incrementAndGet("k", WINDOW)).isEqualTo(1L);
    }

    @Test
    @DisplayName("a null reply counts as a first hit rather than a failure")
    void nullReplyIsTreatedAsFirstHit() {
        assertThat(new RedisRateLimitStore(new StubRedis(() -> null)).incrementAndGet("k", WINDOW))
                .isEqualTo(1L);
    }

}
