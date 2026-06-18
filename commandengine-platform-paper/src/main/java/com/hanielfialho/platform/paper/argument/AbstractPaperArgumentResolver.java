package com.hanielfialho.platform.paper.argument;

import com.hanielfialho.api.argument.ArgumentTypeResolver;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.Objects;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

abstract class AbstractPaperArgumentResolver<T> implements ArgumentTypeResolver<T> {

    private final Class<T> type;
    private final String label;
    private final Function<String, T> lookup;

    AbstractPaperArgumentResolver(Class<T> type, String label, Function<String, T> lookup) {
        this.type = Objects.requireNonNull(type, "type");
        this.label = Objects.requireNonNull(label, "label");
        this.lookup = Objects.requireNonNull(lookup, "lookup");
    }

    @Override
    public final @NotNull Class<T> type() {
        return type;
    }

    @Override
    public @NotNull ArgumentType<?> argumentType() {
        return StringArgumentType.word();
    }

    @Override
    public @NotNull T resolve(@NotNull CommandContext<?> context, @NotNull String name) {
        Objects.requireNonNull(context, "context");
        var argumentName = Objects.requireNonNull(name, "name");
        var raw = StringArgumentType.getString(context, argumentName);
        return resolveRaw(raw);
    }

    @Override
    public @NotNull T resolveDefault(@NotNull CommandContext<?> context, @NotNull String input) {
        Objects.requireNonNull(context, "context");
        return resolveRaw(Objects.requireNonNull(input, "input"));
    }

    private @NotNull T resolveRaw(String raw) {
        var resolved = lookup.apply(raw);
        if (resolved == null) {
            throw new IllegalArgumentException("Unknown " + label + ": " + raw);
        }
        return resolved;
    }
}
