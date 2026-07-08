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
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

final class LocalDateArgumentResolverTest {

    private final ArgumentTypeResolver<LocalDate> resolver = new ArgumentTypeResolver<>() {
        @Override
        public @NotNull Class<LocalDate> type() {
            return LocalDate.class;
        }

        @Override
        public @NotNull ArgumentType<?> argumentType() {
            return StringArgumentType.greedyString();
        }

        @Override
        public @NotNull LocalDate resolve(@NotNull CommandContext<?> context, @NotNull String name) {
            return LocalDate.parse(StringArgumentType.getString(context, name));
        }
    };

    @Test
    void validDate() throws Exception {
        var captured = new AtomicReference<LocalDate>();
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.argument("date", resolver.argumentType())
                        .executes(ctx -> {
                            captured.set(resolver.resolve(ctx, "date"));
                            return 1;
                        })));
        dispatcher.execute("root 2024-01-15", new Object());
        assertThat(captured.get()).isEqualTo(LocalDate.parse("2024-01-15"));
    }

    @Test
    void invalidDate() {
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.argument("date", resolver.argumentType())
                        .executes(ctx -> {
                            resolver.resolve(ctx, "date");
                            return 1;
                        })));
        assertThatThrownBy(() -> dispatcher.execute("root not-a-date", new Object()))
                .isInstanceOf(java.time.format.DateTimeParseException.class);
    }

    @Test
    void differentFormat() {
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.argument("date", resolver.argumentType())
                        .executes(ctx -> {
                            resolver.resolve(ctx, "date");
                            return 1;
                        })));
        assertThatThrownBy(() -> dispatcher.execute("root 01/15/2024", new Object()))
                .isInstanceOf(java.time.format.DateTimeParseException.class);
    }

    @Test
    void leapYear() throws Exception {
        var captured = new AtomicReference<LocalDate>();
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.argument("date", resolver.argumentType())
                        .executes(ctx -> {
                            captured.set(resolver.resolve(ctx, "date"));
                            return 1;
                        })));
        dispatcher.execute("root 2024-02-29", new Object());
        assertThat(captured.get()).isEqualTo(LocalDate.parse("2024-02-29"));
    }
}
