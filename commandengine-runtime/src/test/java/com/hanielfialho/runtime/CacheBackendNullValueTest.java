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
import org.junit.jupiter.api.Test;

final class CacheBackendNullValueTest {

    @Test
    void rejectsNullKey() {
        var cache = new CaffeineCacheBackend<String, String>(Duration.ofMinutes(1), 100);

        assertThatThrownBy(() -> cache.get(null, k -> "value"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("key");
    }

    @Test
    void rejectsNullLoader() {
        var cache = new CaffeineCacheBackend<String, String>(Duration.ofMinutes(1), 100);

        assertThatThrownBy(() -> cache.get("key", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("loader");
    }

    @Test
    void computeReturnsNull() {
        var cache = new CaffeineCacheBackend<String, String>(Duration.ofMinutes(1), 100);

        String first = cache.get("key", k -> null);
        assertThat(first).isNull();

        String second = cache.get("key", k -> "fallback");
        assertThat(second).isEqualTo("fallback");
    }

    @Test
    void clearErrorMessage() {
        var cache = new CaffeineCacheBackend<String, String>(Duration.ofMinutes(1), 100);

        assertThatThrownBy(() -> cache.get(null, k -> "value"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("key");
        assertThatThrownBy(() -> cache.get("key", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("loader");
        assertThatThrownBy(() -> cache.get(null, null)).isInstanceOf(NullPointerException.class);
    }
}
