package com.tourpackage.api.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InMemoryRateLimitStoreTest {

    private final InMemoryRateLimitStore store = new InMemoryRateLimitStore();
    private static final Duration WINDOW = Duration.ofMinutes(1);

    @Test
    @DisplayName("counts up from one")
    void countsUp() {
        assertThat(store.incrementAndGet("a", WINDOW)).isEqualTo(1);
        assertThat(store.incrementAndGet("a", WINDOW)).isEqualTo(2);
        assertThat(store.incrementAndGet("a", WINDOW)).isEqualTo(3);
    }

    @Test
    @DisplayName("different callers have independent budgets")
    void keysAreIndependent() {
        store.incrementAndGet("ip-1", WINDOW);
        store.incrementAndGet("ip-1", WINDOW);

        assertThat(store.incrementAndGet("ip-2", WINDOW)).isEqualTo(1);
    }

    @Test
    @DisplayName("a shorter window starts a separate count")
    void windowIsPartOfTheKey() {
        store.incrementAndGet("a", WINDOW);
        // A different window length must not inherit the previous count, or a
        // config change would leave callers pre-throttled.
        assertThat(store.incrementAndGet("a", Duration.ofSeconds(1))).isEqualTo(1);
    }

    @Test
    @DisplayName("the count resets when the window rolls over")
    void windowRollsOver() throws InterruptedException {
        Duration shortWindow = Duration.ofMillis(200);
        store.incrementAndGet("a", shortWindow);
        store.incrementAndGet("a", shortWindow);

        // Wait past the boundary rather than sleeping exactly one window, which
        // can land inside the same bucket and make this flaky.
        Thread.sleep(450);

        assertThat(store.incrementAndGet("a", shortWindow)).isEqualTo(1);
    }

    @Test
    @DisplayName("concurrent requests are all counted — no lost updates")
    void isThreadSafe() throws InterruptedException {
        int threads = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    store.incrementAndGet("shared", WINDOW);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        // A rate limiter that loses increments under load lets an attacker
        // through precisely when it matters most.
        assertThat(store.incrementAndGet("shared", WINDOW)).isEqualTo(threads + 1);
    }

}
