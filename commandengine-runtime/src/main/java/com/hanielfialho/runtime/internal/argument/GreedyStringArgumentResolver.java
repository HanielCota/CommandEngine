package com.hanielfialho.runtime.internal.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import org.jetbrains.annotations.NotNull;

public final class GreedyStringArgumentResolver extends StringArgumentResolver {

    @Override
    public @NotNull ArgumentType<?> argumentType() {
        return StringArgumentType.greedyString();
    }
}
