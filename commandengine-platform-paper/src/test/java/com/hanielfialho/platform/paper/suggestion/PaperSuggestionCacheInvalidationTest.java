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
package com.hanielfialho.platform.paper.suggestion;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.benmanes.caffeine.cache.Ticker;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.tree.RootCommandNode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

final class PaperSuggestionCacheInvalidationTest {

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

    private static CommandContext<Object> context() {
        return new CommandContext<>(
                new Object(),
                "",
                Map.of(),
                (Command<Object>) ignored -> 1,
                new RootCommandNode<>(),
                List.of(),
                StringRange.at(0),
                null,
                null,
                false);
    }

    @Test
    void cachesResultsAndAvoidsReload() {
        var loads = new AtomicInteger();
        var provider = new TestCachedProvider(loads, () -> true, Duration.ofSeconds(30));
        var ctx = context();

        provider.suggest(ctx, "");
        assertThat(loads).hasValue(1);

        provider.suggest(ctx, "");
        assertThat(loads).hasValue(1);
    }

    @Test
    void returnsCachedResultsWhenNotSafeToLoad() {
        var loads = new AtomicInteger();
        var safeToLoad = new AtomicBoolean(true);
        var provider = new TestCachedProvider(loads, safeToLoad::get, Duration.ofSeconds(30));
        var ctx = context();

        var first = provider.suggest(ctx, "");
        assertThat(loads).hasValue(1);

        safeToLoad.set(false);
        var second = provider.suggest(ctx, "");
        assertThat(second).isEqualTo(first);
        assertThat(loads).hasValue(1);
    }

    @Test
    void returnsEmptyWhenUnsafeAndCacheEmpty() {
        var loads = new AtomicInteger();
        var provider = new TestCachedProvider(loads, () -> false, Duration.ofSeconds(30));
        var ctx = context();

        var result = provider.suggest(ctx, "");

        assertThat(result).isEmpty();
        assertThat(loads).hasValue(0);
    }

    @Test
    void safeToLoadReloadsAfterUnsafePeriod() {
        var loads = new AtomicInteger();
        var safeToLoad = new AtomicBoolean(true);
        var provider = new TestCachedProvider(loads, safeToLoad::get, Duration.ofSeconds(30));
        var ctx = context();

        provider.suggest(ctx, "");
        assertThat(loads).hasValue(1);

        safeToLoad.set(false);
        provider.suggest(ctx, "");
        assertThat(loads).hasValue(1);

        safeToLoad.set(true);
        provider.suggest(ctx, "");
        assertThat(loads).hasValue(1);
    }

    @Test
    void ttlExpires() {
        var loads = new AtomicInteger();
        var ticker = new FakeTicker();
        var provider = new TestCachedProvider(loads, () -> true, Duration.ofMillis(30), ticker);
        var ctx = context();

        provider.suggest(ctx, "");
        assertThat(loads).hasValue(1);

        ticker.advance(Duration.ofMillis(60));

        provider.suggest(ctx, "");
        assertThat(loads).hasValue(2);
    }

    private static final class TestCachedProvider extends CachedSuggestionProvider {

        private final AtomicInteger loads;

        TestCachedProvider(AtomicInteger loads, BooleanSupplier safeToLoad, Duration ttl) {
            super(ttl, 16, safeToLoad);
            this.loads = loads;
        }

        TestCachedProvider(AtomicInteger loads, BooleanSupplier safeToLoad, Duration ttl, Ticker ticker) {
            super(ttl, 16, safeToLoad, ticker);
            this.loads = loads;
        }

        @Override
        protected @NonNull List<String> loadSuggestions() {
            loads.incrementAndGet();
            return List.of("spawn", "shop");
        }
    }
}
