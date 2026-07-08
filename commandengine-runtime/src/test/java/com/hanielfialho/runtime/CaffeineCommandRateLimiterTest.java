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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.benmanes.caffeine.cache.Ticker;
import com.hanielfialho.api.command.CommandPath;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.runtime.internal.rate.CaffeineCommandRateLimiter;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

final class CaffeineCommandRateLimiterTest {

    private static final CommandPath PATH = new CommandPath(new String[] {"guild", "create"});

    @Test
    void allowsUpToMaxExecutionsWithinWindow() {
        var limiter = new CaffeineCommandRateLimiter(Duration.ofMinutes(1), 3, 100);
        var source = new TestSource("player");

        assertThat(limiter.tryAcquire(source, PATH)).isTrue();
        assertThat(limiter.tryAcquire(source, PATH)).isTrue();
        assertThat(limiter.tryAcquire(source, PATH)).isTrue();
        assertThat(limiter.tryAcquire(source, PATH)).isFalse();
    }

    @Test
    void differentSendersHaveIndependentCounters() {
        var limiter = new CaffeineCommandRateLimiter(Duration.ofMinutes(1), 1, 100);
        var alice = new TestSource("alice");
        var bob = new TestSource("bob");

        assertThat(limiter.tryAcquire(alice, PATH)).isTrue();
        assertThat(limiter.tryAcquire(bob, PATH)).isTrue();
        assertThat(limiter.tryAcquire(alice, PATH)).isFalse();
        assertThat(limiter.tryAcquire(bob, PATH)).isFalse();
    }

    @Test
    void counterResetsAfterWindow() {
        var ticker = new FakeTicker();
        var limiter = new CaffeineCommandRateLimiter(Duration.ofMillis(50), 1, 100, ticker);
        var source = new TestSource("player");

        assertThat(limiter.tryAcquire(source, PATH)).isTrue();
        assertThat(limiter.tryAcquire(source, PATH)).isFalse();

        ticker.advance(Duration.ofMillis(51));

        assertThat(limiter.tryAcquire(source, PATH)).isTrue();
    }

    @Test
    void rejectsNullSource() {
        var limiter = new CaffeineCommandRateLimiter(Duration.ofMinutes(1), 3, 100);

        assertThatThrownBy(() -> limiter.tryAcquire(null, PATH))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("source");
    }

    @Test
    void rejectsNullPath() {
        var limiter = new CaffeineCommandRateLimiter(Duration.ofMinutes(1), 3, 100);
        var source = new TestSource("player");

        assertThatThrownBy(() -> limiter.tryAcquire(source, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("path");
    }

    @Test
    void rejectedAttemptsDoNotExtendWindow() {
        var ticker = new FakeTicker();
        var limiter = new CaffeineCommandRateLimiter(Duration.ofMillis(300), 1, 100, ticker);
        var source = new TestSource("player");

        assertThat(limiter.tryAcquire(source, PATH)).isTrue();

        for (int i = 0; i < 10; i++) {
            assertThat(limiter.tryAcquire(source, PATH)).isFalse();
            ticker.advance(Duration.ofMillis(10));
        }

        assertThat(limiter.tryAcquire(source, PATH)).isFalse();

        ticker.advance(Duration.ofMillis(201));
        assertThat(limiter.tryAcquire(source, PATH)).isTrue();
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

    private static final class TestSource implements CommandSource {

        private final String name;

        private TestSource(String name) {
            this.name = name;
        }

        @Override
        public boolean hasPermission(String permission) {
            return false;
        }

        @Override
        public Object getHandle() {
            return this;
        }

        @Override
        public void sendMessage(String message) {
            // no-op: test source
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
