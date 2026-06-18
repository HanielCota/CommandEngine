package com.hanielfialho.api.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import org.jetbrains.annotations.NotNull;

/**
 * Resolves a Brigadier argument into the Java type expected by a command method.
 */
public interface ArgumentTypeResolver<T> {

    @NotNull
    Class<T> type();

    @NotNull
    ArgumentType<?> argumentType();

    @NotNull
    T resolve(@NotNull CommandContext<?> context, @NotNull String name);

    default @NotNull T resolveDefault(@NotNull CommandContext<?> context, @NotNull String input) {
        throw new UnsupportedOperationException("Default values are not supported by " + type().getName());
    }

    /**
     * Returns whether this resolver supports {@link #resolveDefault}.
     */
    default boolean supportsDefault() {
        return false;
    }
}
