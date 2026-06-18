package com.hanielfialho.runtime.internal.cache;

import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

public interface CacheBackend<K, V> {

    V get(@NotNull K key, @NotNull Function<K, V> loader);

    void invalidate(@NotNull K key);

    void invalidateAll();
}
