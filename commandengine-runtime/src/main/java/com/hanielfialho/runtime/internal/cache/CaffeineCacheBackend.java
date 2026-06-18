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
