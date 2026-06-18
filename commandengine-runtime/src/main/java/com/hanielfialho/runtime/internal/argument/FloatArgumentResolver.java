package com.hanielfialho.runtime.internal.argument;

import com.hanielfialho.api.argument.ArgumentTypeResolver;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class FloatArgumentResolver implements ArgumentTypeResolver<Float> {

    @Override
    public @NotNull Class<Float> type() {
        return Float.class;
    }

    @Override
    public @NotNull ArgumentType<?> argumentType() {
        return FloatArgumentType.floatArg();
    }

    @Override
    public @NotNull Float resolve(@NotNull CommandContext<?> context, @NotNull String name) {
        Objects.requireNonNull(context, "context");
        return FloatArgumentType.getFloat(context, Objects.requireNonNull(name, "name"));
    }

    @Override
    public boolean supportsDefault() {
        return true;
    }
}
