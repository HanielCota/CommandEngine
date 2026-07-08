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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

final class CachedSuggestionProviderTest {

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
    void cachesLoadedSuggestionsAndFiltersByRemaining() {
        var loads = new AtomicInteger();
        var provider = new TestSuggestionProvider(loads);
        var context = context();

        assertThat(provider.suggest(context, "s")).containsExactly("spawn", "shop");
        assertThat(provider.suggest(context, "sp")).containsExactly("spawn");

        assertThat(loads).hasValue(1);
    }

    @Test
    void rejectsNonPositiveMaximumSize() {
        assertThatThrownBy(() -> new TestSuggestionProvider(new AtomicInteger(), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maximumSize must be positive");
    }

    private static final class TestSuggestionProvider extends CachedSuggestionProvider {

        private final AtomicInteger loads;

        private TestSuggestionProvider(AtomicInteger loads) {
            this(loads, 16);
        }

        private TestSuggestionProvider(AtomicInteger loads, long maximumSize) {
            super(Duration.ofSeconds(2), maximumSize);
            this.loads = loads;
        }

        @Override
        protected @NonNull List<String> loadSuggestions() {
            loads.incrementAndGet();
            return List.of("spawn", "shop", "arena");
        }
    }
}
