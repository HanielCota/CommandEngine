package com.hanielfialho.platform.paper.suggestion;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hanielfialho.api.suggestion.SuggestionProvider;
import com.mojang.brigadier.context.CommandContext;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public abstract class CachedSuggestionProvider implements SuggestionProvider {

    private static final String CACHE_KEY = "suggestions";

    private final Cache<String, List<String>> cache;

    protected CachedSuggestionProvider(@NotNull Duration ttl) {
        this(ttl, 16);
    }

    protected CachedSuggestionProvider(@NotNull Duration ttl, long maximumSize) {
        if (maximumSize < 1) {
            throw new IllegalArgumentException("maximumSize must be positive");
        }
        cache = Caffeine.newBuilder()
                .expireAfterWrite(Objects.requireNonNull(ttl, "ttl"))
                .maximumSize(maximumSize)
                .build();
    }

    @Override
    public final @NotNull List<String> suggest(@NotNull CommandContext<?> context, @NotNull String remaining) {
        Objects.requireNonNull(context, "context");
        var normalizedRemaining = Objects.requireNonNull(remaining, "remaining").toLowerCase(Locale.ROOT);
        var values = cache.get(CACHE_KEY, key -> List.copyOf(loadSuggestions()));
        if (normalizedRemaining.isBlank()) {
            return values;
        }
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalizedRemaining))
                .toList();
    }

    protected abstract @NotNull List<String> loadSuggestions();
}
