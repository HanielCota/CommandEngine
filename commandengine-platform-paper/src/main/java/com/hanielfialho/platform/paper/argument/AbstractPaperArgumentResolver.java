/*
 * Copyright (c) 2026 Haniel Fialho
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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

    @Override
    public boolean supportsDefault() {
        return true;
    }

    private @NotNull T resolveRaw(String raw) {
        var resolved = lookup.apply(raw);
        if (resolved == null) {
            throw new IllegalArgumentException("Unknown " + label + ": " + raw);
        }
        return resolved;
    }
}
