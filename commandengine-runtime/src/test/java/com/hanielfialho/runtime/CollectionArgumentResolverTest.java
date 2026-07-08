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

import com.hanielfialho.runtime.internal.argument.CollectionArgumentResolver;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class CollectionArgumentResolverTest {

    private final CollectionArgumentResolver resolver = new CollectionArgumentResolver();

    @Test
    void typeIsList() {
        assertThat(resolver.type()).isEqualTo(List.class);
    }

    @Test
    void resolvesCommaSeparatedValues() throws Exception {
        var captured = new AtomicReference<List<String>>();
        execute("cmd a,b,c", captured);
        assertThat(captured.get()).containsExactly("a", "b", "c");
    }

    @Test
    void trimsWhitespace() throws Exception {
        var captured = new AtomicReference<List<String>>();
        execute("cmd a , b , c", captured);
        assertThat(captured.get()).containsExactly("a", "b", "c");
    }

    @Test
    void emptyListForBlankInput() throws Exception {
        var captured = new AtomicReference<List<String>>();
        execute("cmd  ", captured);
        assertThat(captured.get()).isEmpty();
    }

    @Test
    void singleValue() throws Exception {
        var captured = new AtomicReference<List<String>>();
        execute("cmd only", captured);
        assertThat(captured.get()).containsExactly("only");
    }

    private void execute(String input, AtomicReference<List<String>> captured) throws Exception {
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("cmd")
                .then(RequiredArgumentBuilder.argument("list", resolver.argumentType())
                        .executes(ctx -> {
                            captured.set(resolver.resolve(ctx, "list"));
                            return 1;
                        })));
        dispatcher.execute(input, new Object());
    }
}
