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
