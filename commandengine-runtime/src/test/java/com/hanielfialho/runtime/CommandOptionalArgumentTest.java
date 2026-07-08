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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class CommandOptionalArgumentTest {

    @Test
    void optionalAbsentUsesDefault() throws Exception {
        var resolver = new ArgumentTypeResolver<String>() {
            @Override
            public Class<String> type() {
                return String.class;
            }

            @Override
            public StringArgumentType argumentType() {
                return StringArgumentType.word();
            }

            @Override
            public String resolve(CommandContext<?> context, String name) {
                return context.getArgument(name, String.class);
            }

            @Override
            public String resolveDefault(CommandContext<?> context, String input) {
                return "default";
            }

            @Override
            public boolean supportsDefault() {
                return true;
            }
        };
        assertThat(resolver.supportsDefault()).isTrue();
        assertThat(resolver.resolveDefault(null, "")).isEqualTo("default");
    }

    @Test
    void optionalPresentResolvesValue() throws Exception {
        var captured = new AtomicReference<String>();
        var resolver = new ArgumentTypeResolver<String>() {
            @Override
            public Class<String> type() {
                return String.class;
            }

            @Override
            public StringArgumentType argumentType() {
                return StringArgumentType.word();
            }

            @Override
            public String resolve(CommandContext<?> context, String name) {
                return context.getArgument(name, String.class);
            }

            @Override
            public String resolveDefault(CommandContext<?> context, String input) {
                return "fallback";
            }

            @Override
            public boolean supportsDefault() {
                return true;
            }
        };
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("cmd")
                .then(RequiredArgumentBuilder.argument("value", resolver.argumentType())
                        .executes(ctx -> {
                            captured.set(resolver.resolve(ctx, "value"));
                            return 1;
                        })));
        dispatcher.execute("cmd present", new Object());
        assertThat(captured.get()).isEqualTo("present");
    }

    @Test
    void optionalBeforeRequiredRejected() {
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("cmd")
                .then(RequiredArgumentBuilder.argument("opt", StringArgumentType.word())
                        .executes(ctx -> 1)));
        assertThatThrownBy(() -> dispatcher.execute("cmd", new Object())).isInstanceOf(CommandSyntaxException.class);
    }

    @Test
    void errorInOptionalDoesNotMaskNext() {
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("cmd")
                .then(RequiredArgumentBuilder.argument("first", StringArgumentType.word())
                        .executes(ctx -> 1)));
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("cmd")
                .then(LiteralArgumentBuilder.<Object>literal("sub").executes(ctx -> 1)));
        var parse = dispatcher.parse("cmd sub", new Object());
        assertThat(parse.getExceptions()).isEmpty();
    }
}
