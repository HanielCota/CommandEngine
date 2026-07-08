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
package com.hanielfialho.api.suggestion;

import static org.assertj.core.api.Assertions.assertThat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.CommandContextBuilder;
import java.util.List;
import org.junit.jupiter.api.Test;

final class CommandSuggestionTest {

    @Test
    void emptySuggestions() {
        SuggestionProvider provider = (ctx, remaining) -> List.of();
        assertThat(provider.suggest(new SuggestionContext(mockContext(), ""))).isEmpty();
    }

    @Test
    void filteredByPrefix() {
        SuggestionProvider provider = (ctx, remaining) -> List.of("apple", "banana", "avocado").stream()
                .filter(s -> s.startsWith(remaining))
                .toList();
        var result = provider.suggest(new SuggestionContext(mockContext(), "a"));
        assertThat(result).containsExactly("apple", "avocado");
    }

    @Test
    void allSuggestionsWhenEmptyPrefix() {
        SuggestionProvider provider = (ctx, remaining) -> List.of("one", "two", "three");
        var result = provider.suggest(new SuggestionContext(mockContext(), ""));
        assertThat(result).containsExactly("one", "two", "three");
    }

    @Test
    void directExecutorCompletesImmediately() {
        var result = SuggestionExecutor.DIRECT.submit(() -> List.of("a", "b"));
        assertThat(result.join()).containsExactly("a", "b");
    }

    @Test
    void suggestionContextStoresContextAndRemaining() {
        var ctx = mockContext();
        var sc = new SuggestionContext(ctx, "test");
        assertThat(sc.context()).isSameAs(ctx);
        assertThat(sc.remaining()).isEqualTo("test");
    }

    @SuppressWarnings("unchecked")
    private static CommandContext<?> mockContext() {
        var dispatcher = new CommandDispatcher<Object>();
        return new CommandContextBuilder<Object>(dispatcher, new Object(), dispatcher.getRoot(), 0).build("test");
    }
}
