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

import com.hanielfialho.runtime.internal.argument.EnumArgumentResolver;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class EnumArgumentResolverTest {

    private enum TestEnum {
        ONE,
        TWO,
        THREE
    }

    private final EnumArgumentResolver<TestEnum> resolver = new EnumArgumentResolver<>(TestEnum.class);

    @Test
    void typeIsEnumClass() {
        assertThat(resolver.type()).isEqualTo(TestEnum.class);
    }

    @Test
    void resolvesValidEnumValue() throws Exception {
        var captured = new AtomicReference<TestEnum>();
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("cmd")
                .then(RequiredArgumentBuilder.argument("val", resolver.argumentType())
                        .executes(ctx -> {
                            captured.set(resolver.resolve(ctx, "val"));
                            return 1;
                        })));
        dispatcher.execute("cmd ONE", new Object());
        assertThat(captured.get()).isEqualTo(TestEnum.ONE);
    }

    @Test
    void resolvesCaseInsensitive() throws Exception {
        var captured = new AtomicReference<TestEnum>();
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("cmd")
                .then(RequiredArgumentBuilder.argument("val", resolver.argumentType())
                        .executes(ctx -> {
                            captured.set(resolver.resolve(ctx, "val"));
                            return 1;
                        })));
        dispatcher.execute("cmd two", new Object());
        assertThat(captured.get()).isEqualTo(TestEnum.TWO);
    }

    @Test
    void throwsForInvalidEnumValue() {
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("cmd")
                .then(RequiredArgumentBuilder.argument("val", resolver.argumentType())
                        .executes(ctx -> {
                            resolver.resolve(ctx, "val");
                            return 1;
                        })));
        assertThatThrownBy(() -> dispatcher.execute("cmd INVALID", new Object()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void providesSuggestions() throws Exception {
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("cmd")
                .then(RequiredArgumentBuilder.argument("val", resolver.argumentType())
                        .suggests(resolver::listSuggestions)
                        .executes(ctx -> 1)));
        var parse = dispatcher.parse("cmd ", new Object());
        var suggestions = dispatcher.getCompletionSuggestions(parse).join();
        assertThat(suggestions.getList()).extracting(s -> s.getText()).containsExactlyInAnyOrder("one", "two", "three");
    }
}
