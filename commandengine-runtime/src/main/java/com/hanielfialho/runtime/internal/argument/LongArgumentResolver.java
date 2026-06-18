package com.hanielfialho.runtime.internal.argument;

import com.hanielfialho.api.argument.ArgumentTypeResolver;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class LongArgumentResolver implements ArgumentTypeResolver<Long> {

    @Override
    public @NotNull Class<Long> type() {
        return Long.class;
    }

    @Override
    public @NotNull ArgumentType<?> argumentType() {
        return LongArgumentType.longArg();
    }

    @Override
    public @NotNull Long resolve(@NotNull CommandContext<?> context, @NotNull String name) {
        Objects.requireNonNull(context, "context");
        return LongArgumentType.getLong(context, Objects.requireNonNull(name, "name"));
    }

    @Override
    public boolean supportsDefault() {
        return true;
    }
}
