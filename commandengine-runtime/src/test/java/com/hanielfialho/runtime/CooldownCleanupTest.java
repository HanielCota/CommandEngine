/*
 * Copyright (c) 2026 Haniel Fialho
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.hanielfialho.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.benmanes.caffeine.cache.Ticker;
import com.hanielfialho.api.command.CommandPath;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.runtime.internal.rate.CaffeineCommandRateLimiter;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

final class CooldownCleanupTest {

    private static final CommandPath PATH = new CommandPath("cmd");

    @Test
    void expiredEntriesRemoved() {
        var ticker = new FakeTicker();
        var limiter = new CaffeineCommandRateLimiter(Duration.ofMillis(100), 1, 100, ticker);
        var source = testSource("player");

        assertThat(limiter.tryAcquire(source, PATH)).isTrue();
        assertThat(limiter.tryAcquire(source, PATH)).isFalse();

        ticker.advance(Duration.ofMillis(200));

        assertThat(limiter.tryAcquire(source, PATH)).isTrue();
    }

    @Test
    void cleanupDoesNotRemoveActiveEntries() {
        var ticker = new FakeTicker();
        var limiter = new CaffeineCommandRateLimiter(Duration.ofMinutes(10), 2, 100, ticker);
        var source = testSource("player");

        assertThat(limiter.tryAcquire(source, PATH)).isTrue();
        assertThat(limiter.tryAcquire(source, PATH)).isTrue();

        ticker.advance(Duration.ofSeconds(30));

        assertThat(limiter.tryAcquire(source, PATH)).isFalse();
    }

    @Test
    void concurrentCleanup() throws Exception {
        var limiter = new CaffeineCommandRateLimiter(Duration.ofSeconds(10), 5, 100);
        var latch = new CountDownLatch(2);
        var pool = Executors.newFixedThreadPool(2);

        var pathA = new CommandPath("cmd-a");
        var pathB = new CommandPath("cmd-b");

        pool.submit(() -> {
            var source = testSource("user1");
            for (int i = 0; i < 10; i++) {
                limiter.tryAcquire(source, pathA);
            }
            latch.countDown();
        });

        pool.submit(() -> {
            var source = testSource("user2");
            for (int i = 0; i < 10; i++) {
                limiter.tryAcquire(source, pathB);
            }
            latch.countDown();
        });

        latch.await();
        pool.shutdown();
    }

    private static CommandSource testSource(String name) {
        return new CommandSource() {
            @Override
            public boolean hasPermission(String permission) {
                return false;
            }

            @Override
            public Object getHandle() {
                return this;
            }

            @Override
            public void sendMessage(String message) {}

            @Override
            public String getName() {
                return name;
            }
        };
    }

    private static final class FakeTicker implements Ticker {

        private final AtomicLong nanos = new AtomicLong();

        @Override
        public long read() {
            return nanos.get();
        }

        private void advance(Duration duration) {
            nanos.addAndGet(duration.toNanos());
        }
    }
}
