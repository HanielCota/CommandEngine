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
package com.hanielfialho.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hanielfialho.api.argument.ArgumentTypeResolver;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

final class ShortArgumentResolverTest {

    private final ArgumentTypeResolver<Short> resolver = new ArgumentTypeResolver<>() {
        @Override
        public @NotNull Class<Short> type() {
            return Short.class;
        }

        @Override
        public @NotNull ArgumentType<?> argumentType() {
            return IntegerArgumentType.integer(Short.MIN_VALUE, Short.MAX_VALUE);
        }

        @Override
        public @NotNull Short resolve(@NotNull CommandContext<?> context, @NotNull String name) {
            return (short) IntegerArgumentType.getInteger(context, name);
        }
    };

    @Test
    void validShort() throws Exception {
        var captured = new AtomicReference<Short>();
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.argument("val", resolver.argumentType())
                        .executes(ctx -> {
                            captured.set(resolver.resolve(ctx, "val"));
                            return 1;
                        })));
        dispatcher.execute("root 42", new Object());
        assertThat(captured.get()).isEqualTo((short) 42);
    }

    @Test
    void invalidShort() {
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.argument("val", resolver.argumentType())
                        .executes(ctx -> 1)));
        assertThatThrownBy(() -> dispatcher.execute("root abc", new Object()))
                .isInstanceOf(CommandSyntaxException.class);
    }

    @Test
    void boundaryMin() throws Exception {
        var captured = new AtomicReference<Short>();
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.argument("val", resolver.argumentType())
                        .executes(ctx -> {
                            captured.set(resolver.resolve(ctx, "val"));
                            return 1;
                        })));
        dispatcher.execute("root " + Short.MIN_VALUE, new Object());
        assertThat(captured.get()).isEqualTo(Short.MIN_VALUE);
    }

    @Test
    void boundaryMax() throws Exception {
        var captured = new AtomicReference<Short>();
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.argument("val", resolver.argumentType())
                        .executes(ctx -> {
                            captured.set(resolver.resolve(ctx, "val"));
                            return 1;
                        })));
        dispatcher.execute("root " + Short.MAX_VALUE, new Object());
        assertThat(captured.get()).isEqualTo(Short.MAX_VALUE);
    }

    @Test
    void outOfRange() {
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.argument("val", resolver.argumentType())
                        .executes(ctx -> 1)));
        assertThatThrownBy(() -> dispatcher.execute("root 99999", new Object()))
                .isInstanceOf(CommandSyntaxException.class);
    }
}
