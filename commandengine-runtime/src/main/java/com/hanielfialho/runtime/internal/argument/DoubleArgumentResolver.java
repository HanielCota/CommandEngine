package com.hanielfialho.runtime.internal.argument;

import com.hanielfialho.api.argument.ArgumentTypeResolver;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class DoubleArgumentResolver implements ArgumentTypeResolver<Double> {

    @Override
    public @NotNull Class<Double> type() {
        return Double.class;
    }

    @Override
    public @NotNull ArgumentType<?> argumentType() {
        return DoubleArgumentType.doubleArg();
    }

    @Override
    public @NotNull Double resolve(@NotNull CommandContext<?> context, @NotNull String name) {
        Objects.requireNonNull(context, "context");
        return DoubleArgumentType.getDouble(context, Objects.requireNonNull(name, "name"));
    }
}
