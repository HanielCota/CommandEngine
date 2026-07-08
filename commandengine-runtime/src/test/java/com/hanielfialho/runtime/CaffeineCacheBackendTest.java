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

import com.hanielfialho.runtime.internal.cache.CaffeineCacheBackend;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class CaffeineCacheBackendTest {

    @Test
    void cachesLoadedValues() {
        var loads = new AtomicInteger();
        var cache = new CaffeineCacheBackend<String, String>(Duration.ofMinutes(1), 16);

        String first = cache.get("name", key -> {
            loads.incrementAndGet();
            return "haniel";
        });
        String second = cache.get("name", key -> {
            loads.incrementAndGet();
            return "other";
        });

        assertThat(first).isEqualTo("haniel");
        assertThat(second).isEqualTo("haniel");
        assertThat(loads).hasValue(1);
    }

    @Test
    void invalidateRemovesSingleKey() {
        var loads = new AtomicInteger();
        var cache = new CaffeineCacheBackend<String, String>(Duration.ofMinutes(1), 16);

        cache.get("name", key -> "value-" + loads.incrementAndGet());
        cache.invalidate("name");

        assertThat(cache.get("name", key -> "value-" + loads.incrementAndGet())).isEqualTo("value-2");
    }

    @Test
    void invalidateAllRemovesAllKeys() {
        var loads = new AtomicInteger();
        var cache = new CaffeineCacheBackend<String, String>(Duration.ofMinutes(1), 16);

        cache.get("first", key -> "value-" + loads.incrementAndGet());
        cache.get("second", key -> "value-" + loads.incrementAndGet());
        cache.invalidateAll();

        assertThat(cache.get("first", key -> "value-" + loads.incrementAndGet()))
                .isEqualTo("value-3");
        assertThat(cache.get("second", key -> "value-" + loads.incrementAndGet()))
                .isEqualTo("value-4");
    }

    @Test
    void rejectsNonPositiveMaximumSize() {
        assertThatThrownBy(() -> new CaffeineCacheBackend<>(Duration.ofMinutes(1), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maximumSize must be positive");
    }
}
