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

import com.hanielfialho.runtime.internal.argument.StringQuotedArgumentResolver;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class StringQuotedArgumentResolverTest {

    private final StringQuotedArgumentResolver resolver = new StringQuotedArgumentResolver();

    @Test
    void typeIsString() {
        assertThat(resolver.type()).isEqualTo(String.class);
    }

    @Test
    void argumentTypeIsGreedyString() {
        assertThat(resolver.argumentType()).isInstanceOf(StringArgumentType.class);
    }

    @Test
    void supportsDefault() {
        assertThat(resolver.supportsDefault()).isTrue();
    }

    @Test
    void resolvesGreedyStringWithSpaces() throws Exception {
        var captured = new AtomicReference<String>();
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("cmd")
                .then(RequiredArgumentBuilder.<Object, String>argument("msg", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            captured.set(StringArgumentType.getString(ctx, "msg"));
                            return 1;
                        })));
        dispatcher.execute("cmd hello brave world", new Object());
        assertThat(captured.get()).isEqualTo("hello brave world");
    }
}
