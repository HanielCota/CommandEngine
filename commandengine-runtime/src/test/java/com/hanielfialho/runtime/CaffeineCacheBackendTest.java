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
