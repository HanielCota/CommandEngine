package com.hanielfialho.platform.paper.suggestion;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hanielfialho.api.suggestion.SuggestionProvider;
import com.mojang.brigadier.context.CommandContext;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import org.jetbrains.annotations.NotNull;

public abstract class CachedSuggestionProvider implements SuggestionProvider {

    private static final String CACHE_KEY = "suggestions";

    private final Cache<String, List<String>> cache;
    private final BooleanSupplier isSafeToLoad;

    protected CachedSuggestionProvider(@NotNull Duration ttl) {
        this(ttl, 16);
    }

    protected CachedSuggestionProvider(@NotNull Duration ttl, long maximumSize) {
        this(ttl, maximumSize, () -> true);
    }

    protected CachedSuggestionProvider(@NotNull Duration ttl, long maximumSize, @NotNull BooleanSupplier isSafeToLoad) {
        if (maximumSize < 1) {
            throw new IllegalArgumentException("maximumSize must be positive");
        }
        this.isSafeToLoad = Objects.requireNonNull(isSafeToLoad, "isSafeToLoad");
        cache = Caffeine.newBuilder()
                .expireAfterWrite(Objects.requireNonNull(ttl, "ttl"))
                .maximumSize(maximumSize)
                .build();
    }

    @Override
    public final @NotNull List<String> suggest(@NotNull CommandContext<?> context, @NotNull String remaining) {
        Objects.requireNonNull(context, "context");
        var normalizedRemaining = Objects.requireNonNull(remaining, "remaining").toLowerCase(Locale.ROOT);
        List<String> values;
        if (isSafeToLoad.getAsBoolean()) {
            values = cache.get(CACHE_KEY, key -> List.copyOf(loadSuggestions()));
        } else {
            values = cache.getIfPresent(CACHE_KEY);
            if (values == null) {
                values = List.of();
            }
        }
        if (normalizedRemaining.isBlank()) {
            return values;
        }
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalizedRemaining))
                .toList();
    }

    protected abstract @NotNull List<String> loadSuggestions();
}
