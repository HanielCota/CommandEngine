package com.hanielfialho.runtime.internal.argument;

import com.hanielfialho.api.argument.ArgumentTypeResolver;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class IntegerArgumentResolver implements ArgumentTypeResolver<Integer> {

    @Override
    public @NotNull Class<Integer> type() {
        return Integer.class;
    }

    @Override
    public @NotNull ArgumentType<?> argumentType() {
        return IntegerArgumentType.integer();
    }

    @Override
    public @NotNull Integer resolve(@NotNull CommandContext<?> context, @NotNull String name) {
        Objects.requireNonNull(context, "context");
        return IntegerArgumentType.getInteger(context, Objects.requireNonNull(name, "name"));
    }
}
