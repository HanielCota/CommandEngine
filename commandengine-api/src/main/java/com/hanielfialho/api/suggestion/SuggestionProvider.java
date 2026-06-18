package com.hanielfialho.api.suggestion;

import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public interface SuggestionProvider {

    @NotNull
    List<String> suggest(@NotNull CommandContext<?> context, @NotNull String remaining);

    default @NotNull List<String> suggest(@NotNull SuggestionContext context) {
        return suggest(context.context(), context.remaining());
    }
}
