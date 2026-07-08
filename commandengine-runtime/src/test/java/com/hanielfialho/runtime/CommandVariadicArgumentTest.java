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

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class CommandVariadicArgumentTest {

    @Test
    void capturesRestOfLine() throws Exception {
        var captured = new AtomicReference<String>();
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("say")
                .then(RequiredArgumentBuilder.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            captured.set(ctx.getArgument("message", String.class));
                            return 1;
                        })));
        dispatcher.execute("say Hello world! This is a test.", new Object());
        assertThat(captured.get()).isEqualTo("Hello world! This is a test.");
    }

    @Test
    void variadicEmpty() {
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("say")
                .then(RequiredArgumentBuilder.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> 1)));
        var parse = dispatcher.parse("say", new Object());
        assertThat(parse.getExceptions()).isEmpty();
    }

    @Test
    void variadicWithQuotes() throws Exception {
        var captured = new AtomicReference<String>();
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("say")
                .then(RequiredArgumentBuilder.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            captured.set(ctx.getArgument("message", String.class));
                            return 1;
                        })));
        dispatcher.execute("say \"quoted string\" and more", new Object());
        assertThat(captured.get()).isEqualTo("\"quoted string\" and more");
    }

    @Test
    void variadicBeforeRequiredFails() {
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("test")
                .then(RequiredArgumentBuilder.argument("rest", StringArgumentType.greedyString())
                        .then(RequiredArgumentBuilder.argument("after", StringArgumentType.word())
                                .executes(ctx -> 1))));
        assertThatThrownBy(() -> dispatcher.execute("test hello world", new Object()))
                .isInstanceOf(CommandSyntaxException.class);
    }
}
