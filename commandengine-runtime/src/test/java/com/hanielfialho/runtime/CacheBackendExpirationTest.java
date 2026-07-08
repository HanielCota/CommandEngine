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
import com.hanielfialho.runtime.internal.cache.CaffeineCacheBackend;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class CacheBackendExpirationTest {

    private static final class FakeTicker implements Ticker {
        private long nanos;

        @Override
        public long read() {
            return nanos;
        }

        void advance(Duration duration) {
            nanos += duration.toNanos();
        }
    }

    @Test
    void expirationByTime() {
        var loads = new AtomicInteger();
        var ticker = new FakeTicker();
        var cache = new CaffeineCacheBackend<String, String>(Duration.ofMillis(5), 100, ticker);

        cache.get("key", k -> "value-" + loads.incrementAndGet());
        assertThat(loads).hasValue(1);

        ticker.advance(Duration.ofMillis(50));

        String result = cache.get("key", k -> "value-" + loads.incrementAndGet());
        assertThat(result).isEqualTo("value-2");
        assertThat(loads).hasValue(2);
    }

    @Test
    void expirationBySize() {
        var loads = new AtomicInteger();
        var cache = new CaffeineCacheBackend<String, String>(Duration.ofMinutes(10), 1);

        cache.get("key1", k -> "v" + loads.incrementAndGet());

        for (int i = 0; i < 100; i++) {
            cache.get("key2", k -> "v" + loads.incrementAndGet());
        }

        String result = cache.get("key1", k -> "v" + loads.incrementAndGet());
        assertThat(result).isEqualTo("v3");
    }

    @Test
    void manualRemoval() {
        var loads = new AtomicInteger();
        var cache = new CaffeineCacheBackend<String, String>(Duration.ofMinutes(10), 100);

        cache.get("key", k -> "value-" + loads.incrementAndGet());
        cache.invalidate("key");

        String result = cache.get("key", k -> "value-" + loads.incrementAndGet());
        assertThat(result).isEqualTo("value-2");
        assertThat(loads).hasValue(2);
    }

    @Test
    void accessRenewsTtl() {
        var loads = new AtomicInteger();
        var ticker = new FakeTicker();
        var cache = new CaffeineCacheBackend<String, String>(Duration.ofMillis(50), 100, ticker);

        cache.get("key", k -> "value-" + loads.incrementAndGet());
        assertThat(loads).hasValue(1);

        ticker.advance(Duration.ofMillis(20));

        cache.get("key", k -> "value-" + loads.incrementAndGet());
        assertThat(loads).hasValue(1);

        ticker.advance(Duration.ofMillis(60));

        String result = cache.get("key", k -> "value-" + loads.incrementAndGet());
        assertThat(result).isEqualTo("value-2");
        assertThat(loads).hasValue(2);
    }
}
