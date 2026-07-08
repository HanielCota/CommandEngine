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
