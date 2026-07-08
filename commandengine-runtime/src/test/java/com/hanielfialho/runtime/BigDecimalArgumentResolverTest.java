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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

final class BigDecimalArgumentResolverTest {

    private final ArgumentTypeResolver<BigDecimal> resolver = new ArgumentTypeResolver<>() {
        @Override
        public @NotNull Class<BigDecimal> type() {
            return BigDecimal.class;
        }

        @Override
        public @NotNull ArgumentType<?> argumentType() {
            return StringArgumentType.string();
        }

        @Override
        public @NotNull BigDecimal resolve(@NotNull CommandContext<?> context, @NotNull String name) {
            return new BigDecimal(StringArgumentType.getString(context, name));
        }
    };

    @Test
    void validBigDecimal() throws Exception {
        var captured = new AtomicReference<BigDecimal>();
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.argument("val", resolver.argumentType())
                        .executes(ctx -> {
                            captured.set(resolver.resolve(ctx, "val"));
                            return 1;
                        })));
        dispatcher.execute("root 123.456", new Object());
        assertThat(captured.get()).isEqualByComparingTo(new BigDecimal("123.456"));
    }

    @Test
    void invalidBigDecimal() {
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.argument("val", resolver.argumentType())
                        .executes(ctx -> {
                            resolver.resolve(ctx, "val");
                            return 1;
                        })));
        assertThatThrownBy(() -> dispatcher.execute("root abc", new Object()))
                .isInstanceOf(NumberFormatException.class);
    }

    @Test
    void negativeValue() throws Exception {
        var captured = new AtomicReference<BigDecimal>();
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.argument("val", resolver.argumentType())
                        .executes(ctx -> {
                            captured.set(resolver.resolve(ctx, "val"));
                            return 1;
                        })));
        dispatcher.execute("root -123.45", new Object());
        assertThat(captured.get()).isEqualByComparingTo(new BigDecimal("-123.45"));
    }

    @Test
    void zero() throws Exception {
        var captured = new AtomicReference<BigDecimal>();
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.argument("val", resolver.argumentType())
                        .executes(ctx -> {
                            captured.set(resolver.resolve(ctx, "val"));
                            return 1;
                        })));
        dispatcher.execute("root 0", new Object());
        assertThat(captured.get()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void scientificNotation() throws Exception {
        var captured = new AtomicReference<BigDecimal>();
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.argument("val", resolver.argumentType())
                        .executes(ctx -> {
                            captured.set(resolver.resolve(ctx, "val"));
                            return 1;
                        })));
        dispatcher.execute("root 1.23e5", new Object());
        assertThat(captured.get()).isEqualByComparingTo(new BigDecimal("1.23e5"));
    }
}
