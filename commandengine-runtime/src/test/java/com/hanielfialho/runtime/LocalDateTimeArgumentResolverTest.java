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
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

final class LocalDateTimeArgumentResolverTest {

    private final ArgumentTypeResolver<LocalDateTime> resolver = new ArgumentTypeResolver<>() {
        @Override
        public @NotNull Class<LocalDateTime> type() {
            return LocalDateTime.class;
        }

        @Override
        public @NotNull ArgumentType<?> argumentType() {
            return StringArgumentType.greedyString();
        }

        @Override
        public @NotNull LocalDateTime resolve(@NotNull CommandContext<?> context, @NotNull String name) {
            return LocalDateTime.parse(StringArgumentType.getString(context, name));
        }
    };

    @Test
    void validDateTime() throws Exception {
        var captured = new AtomicReference<LocalDateTime>();
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.argument("dt", resolver.argumentType())
                        .executes(ctx -> {
                            captured.set(resolver.resolve(ctx, "dt"));
                            return 1;
                        })));
        dispatcher.execute("root 2024-01-15T10:30:00", new Object());
        assertThat(captured.get()).isEqualTo(LocalDateTime.parse("2024-01-15T10:30:00"));
    }

    @Test
    void invalidDateTime() {
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.argument("dt", resolver.argumentType())
                        .executes(ctx -> {
                            resolver.resolve(ctx, "dt");
                            return 1;
                        })));
        assertThatThrownBy(() -> dispatcher.execute("root not-a-date", new Object()))
                .isInstanceOf(java.time.format.DateTimeParseException.class);
    }

    @Test
    void dateOnly() {
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.argument("dt", resolver.argumentType())
                        .executes(ctx -> {
                            resolver.resolve(ctx, "dt");
                            return 1;
                        })));
        assertThatThrownBy(() -> dispatcher.execute("root 2024-01-15", new Object()))
                .isInstanceOf(java.time.format.DateTimeParseException.class);
    }

    @Test
    void withNanos() throws Exception {
        var captured = new AtomicReference<LocalDateTime>();
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.argument("dt", resolver.argumentType())
                        .executes(ctx -> {
                            captured.set(resolver.resolve(ctx, "dt"));
                            return 1;
                        })));
        dispatcher.execute("root 2024-01-15T10:30:00.123456789", new Object());
        assertThat(captured.get()).isEqualTo(LocalDateTime.parse("2024-01-15T10:30:00.123456789"));
    }
}
