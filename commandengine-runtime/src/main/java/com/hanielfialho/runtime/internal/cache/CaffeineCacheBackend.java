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
package com.hanielfialho.runtime.internal.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

public final class CaffeineCacheBackend<K, V> implements CacheBackend<K, V> {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);
    private static final long DEFAULT_MAXIMUM_SIZE = 10_000L;

    private final Cache<K, V> cache;

    public CaffeineCacheBackend() {
        this(DEFAULT_TTL, DEFAULT_MAXIMUM_SIZE);
    }

    public CaffeineCacheBackend(@NotNull Duration ttl, long maximumSize) {
        if (maximumSize < 1) {
            throw new IllegalArgumentException("maximumSize must be positive");
        }
        cache = Caffeine.newBuilder()
                .expireAfterWrite(Objects.requireNonNull(ttl, "ttl"))
                .maximumSize(maximumSize)
                .build();
    }

    @Override
    public V get(@NotNull K key, @NotNull Function<K, V> loader) {
        return cache.get(Objects.requireNonNull(key, "key"), Objects.requireNonNull(loader, "loader"));
    }

    @Override
    public void invalidate(@NotNull K key) {
        cache.invalidate(Objects.requireNonNull(key, "key"));
    }

    @Override
    public void invalidateAll() {
        cache.invalidateAll();
    }
}
