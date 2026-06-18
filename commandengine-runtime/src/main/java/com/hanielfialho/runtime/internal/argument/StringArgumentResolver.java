package com.hanielfialho.runtime.internal.argument;

import com.hanielfialho.api.argument.ArgumentTypeResolver;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public class StringArgumentResolver implements ArgumentTypeResolver<String> {

    @Override
    public @NotNull Class<String> type() {
        return String.class;
    }

    @Override
    public @NotNull ArgumentType<?> argumentType() {
        return StringArgumentType.string();
    }

    @Override
    public @NotNull String resolve(@NotNull CommandContext<?> context, @NotNull String name) {
        Objects.requireNonNull(context, "context");
        return StringArgumentType.getString(context, Objects.requireNonNull(name, "name"));
    }

    @Override
    public boolean supportsDefault() {
        return true;
    }
}
