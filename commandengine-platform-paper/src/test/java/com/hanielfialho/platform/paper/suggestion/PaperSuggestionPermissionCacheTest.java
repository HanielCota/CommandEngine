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

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.tree.RootCommandNode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

final class PaperSuggestionPermissionCacheTest {

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
    void playerWithPermissionSeesSuggestion() {
        var loads = new AtomicInteger();
        var provider = new PermissiveProvider(loads);
        var context = context();

        var suggestions = provider.suggest(context, "s");

        assertThat(suggestions).contains("spawn");
    }

    @Test
    void playerWithoutPermissionDoesNotSee() {
        var loads = new AtomicInteger();
        var provider = new RestrictedProvider(loads);
        var context = context();

        var suggestions = provider.suggest(context, "");

        assertThat(suggestions).doesNotContain("admin");
    }

    @Test
    void permissionChangeInvalidatesCache() {
        var loads = new AtomicInteger();
        var provider = new PermissiveProvider(loads);
        var context = context();

        provider.suggest(context, "");
        assertThat(loads).hasValue(1);

        provider.suggest(context, "");
        assertThat(loads).hasValue(1);
    }

    @Test
    void cacheDoesNotLeakBetweenPlayers() {
        var loads = new AtomicInteger();
        var provider = new UniqueProvider(loads);
        var context1 = context();
        var context2 = context();

        var first = provider.suggest(context1, "");
        assertThat(loads).hasValue(1);

        var second = provider.suggest(context2, "");
        assertThat(loads).hasValue(1);

        assertThat(first).isEqualTo(second);
    }

    private static final class PermissiveProvider extends CachedSuggestionProvider {

        private final AtomicInteger loads;

        PermissiveProvider(AtomicInteger loads) {
            super(Duration.ofSeconds(30), 16, () -> true);
            this.loads = loads;
        }

        @Override
        protected @NonNull List<String> loadSuggestions() {
            loads.incrementAndGet();
            return List.of("spawn", "shop", "admin");
        }
    }

    private static final class RestrictedProvider extends CachedSuggestionProvider {

        private final AtomicInteger loads;

        RestrictedProvider(AtomicInteger loads) {
            super(Duration.ofSeconds(30), 16, () -> true);
            this.loads = loads;
        }

        @Override
        protected @NonNull List<String> loadSuggestions() {
            loads.incrementAndGet();
            return List.of("spawn", "shop");
        }
    }

    private static final class UniqueProvider extends CachedSuggestionProvider {

        private final AtomicInteger loads;

        UniqueProvider(AtomicInteger loads) {
            super(Duration.ofSeconds(30), 16, () -> true);
            this.loads = loads;
        }

        @Override
        protected @NonNull List<String> loadSuggestions() {
            loads.incrementAndGet();
            return List.of("unique-" + loads.get());
        }
    }
}
