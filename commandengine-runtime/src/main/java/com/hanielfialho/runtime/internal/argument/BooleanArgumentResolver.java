package com.hanielfialho.runtime.internal.argument;

import com.hanielfialho.api.argument.ArgumentTypeResolver;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class BooleanArgumentResolver implements ArgumentTypeResolver<Boolean> {

    @Override
    public @NotNull Class<Boolean> type() {
        return Boolean.class;
    }

    @Override
    public @NotNull ArgumentType<?> argumentType() {
        return BoolArgumentType.bool();
    }

    @Override
    public @NotNull Boolean resolve(@NotNull CommandContext<?> context, @NotNull String name) {
        Objects.requireNonNull(context, "context");
        return BoolArgumentType.getBool(context, Objects.requireNonNull(name, "name"));
    }
}
