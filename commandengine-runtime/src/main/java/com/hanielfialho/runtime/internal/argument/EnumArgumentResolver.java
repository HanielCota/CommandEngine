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
package com.hanielfialho.runtime.internal.argument;

import com.hanielfialho.api.argument.ArgumentTypeResolver;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

public final class EnumArgumentResolver<E extends Enum<E>> implements ArgumentTypeResolver<E> {

    private final Class<E> enumClass;
    private final E[] constants;

    @SuppressWarnings("unchecked")
    public EnumArgumentResolver(@NotNull Class<E> enumClass) {
        this.enumClass = Objects.requireNonNull(enumClass, "enumClass");
        this.constants = enumClass.getEnumConstants();
        if (constants == null) {
            throw new IllegalArgumentException("Not an enum type: " + enumClass);
        }
    }

    @Override
    public @NotNull Class<E> type() {
        return enumClass;
    }

    @Override
    public @NotNull ArgumentType<?> argumentType() {
        return StringArgumentType.word();
    }

    @Override
    public @NotNull E resolve(@NotNull CommandContext<?> context, @NotNull String name) {
        var input = StringArgumentType.getString(context, Objects.requireNonNull(name, "name"));
        for (var constant : constants) {
            if (constant.name().equalsIgnoreCase(input)) {
                return constant;
            }
        }
        throw new IllegalArgumentException("Invalid enum value: " + input + " for " + enumClass.getSimpleName());
    }

    public @NotNull CompletableFuture<Suggestions> listSuggestions(
            @NotNull CommandContext<?> context, @NotNull SuggestionsBuilder builder) {
        var remaining = builder.getRemaining().toLowerCase();
        for (var constant : constants) {
            if (constant.name().toLowerCase().startsWith(remaining)) {
                builder.suggest(constant.name().toLowerCase());
            }
        }
        return builder.buildFuture();
    }
}
