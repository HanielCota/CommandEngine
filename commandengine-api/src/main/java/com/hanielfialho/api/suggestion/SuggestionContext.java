package com.hanielfialho.api.suggestion;

import com.mojang.brigadier.context.CommandContext;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record SuggestionContext(
        @NotNull CommandContext<?> context, @NotNull String remaining) {

    public SuggestionContext {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(remaining, "remaining");
    }
}
