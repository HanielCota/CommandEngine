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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.CommandContextBuilder;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class AbstractPaperArgumentResolverTest {

    @Test
    void typeMatchesConstructorArg() {
        var resolver = new TestResolver();
        assertThat(resolver.type()).isEqualTo(String.class);
    }

    @Test
    void argumentTypeIsStringWord() {
        var resolver = new TestResolver();
        assertThat(resolver.argumentType()).isNotNull();
    }

    @Test
    void supportsDefaultReturnsTrue() {
        var resolver = new TestResolver();
        assertThat(resolver.supportsDefault()).isTrue();
    }

    @Test
    void resolvesValueByLookup() throws Exception {
        var resolver = new TestResolver();
        var captured = new AtomicReference<String>();
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("cmd")
                .then(RequiredArgumentBuilder.argument("arg", resolver.argumentType())
                        .executes(ctx -> {
                            captured.set(resolver.resolve(ctx, "arg"));
                            return 1;
                        })));
        dispatcher.execute("cmd hello", new Object());
        assertThat(captured.get()).isEqualTo("resolved:hello");
    }

    @Test
    void throwsOnUnknownEntity() {
        var failing = new FailingResolver();
        assertThatThrownBy(() -> failing.resolveDefault(mockContext(), "unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown testEntity: unknown");
    }

    @Test
    void resolveDefaultDelegatesToLookup() {
        var resolver = new TestResolver();
        var result = resolver.resolveDefault(mockContext(), "valid");
        assertThat(result).isEqualTo("resolved:valid");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static CommandContext<?> mockContext() {
        var dispatcher = new CommandDispatcher();
        return new CommandContextBuilder(dispatcher, new Object(), dispatcher.getRoot(), 0).build("test");
    }

    private static final class TestResolver extends AbstractPaperArgumentResolver<String> {

        TestResolver() {
            super(String.class, "testEntity", raw -> "resolved:" + raw);
        }
    }

    private static final class FailingResolver extends AbstractPaperArgumentResolver<String> {

        FailingResolver() {
            super(String.class, "testEntity", raw -> null);
        }
    }
}
