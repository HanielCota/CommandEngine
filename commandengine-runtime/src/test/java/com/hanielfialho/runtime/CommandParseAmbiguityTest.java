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

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class CommandParseAmbiguityTest {

    @Test
    void commandUserAdd() throws Exception {
        var captured = new AtomicReference<String>();
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("user")
                .then(LiteralArgumentBuilder.<Object>literal("add").executes(ctx -> {
                    captured.set("add");
                    return 1;
                })));
        dispatcher.execute("user add", new Object());
        assertThat(captured.get()).isEqualTo("add");
    }

    @Test
    void commandUserAddRole() throws Exception {
        var captured = new AtomicReference<String>();
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("user")
                .then(LiteralArgumentBuilder.<Object>literal("addrole").executes(ctx -> {
                    captured.set("addrole");
                    return 1;
                })));
        dispatcher.execute("user addrole", new Object());
        assertThat(captured.get()).isEqualTo("addrole");
    }

    @Test
    void subcommandWithSimilarPrefix() throws Exception {
        var captured = new AtomicReference<String>();
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("user")
                .then(LiteralArgumentBuilder.<Object>literal("add").executes(ctx -> {
                    captured.set("add");
                    return 1;
                }))
                .then(LiteralArgumentBuilder.<Object>literal("addrole").executes(ctx -> {
                    captured.set("addrole");
                    return 1;
                })));
        dispatcher.execute("user addrole", new Object());
        assertThat(captured.get()).isEqualTo("addrole");
        dispatcher.execute("user add", new Object());
        assertThat(captured.get()).isEqualTo("add");
    }

    @Test
    void ambiguousAlias() {
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("group")
                .then(LiteralArgumentBuilder.<Object>literal("add").executes(ctx -> 1)));
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("group")
                .then(LiteralArgumentBuilder.<Object>literal("add").executes(ctx -> 2)));
        var parse = dispatcher.parse("group add", new Object());
        assertThat(parse.getExceptions()).isEmpty();
    }

    @Test
    void predictableTiebreaker() throws Exception {
        var dispatcher = new CommandDispatcher<Object>();
        var ref = new AtomicReference<String>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("cmd")
                .then(LiteralArgumentBuilder.<Object>literal("a").executes(ctx -> {
                    ref.set("first");
                    return 1;
                })));
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("cmd")
                .then(LiteralArgumentBuilder.<Object>literal("b").executes(ctx -> {
                    ref.set("second");
                    return 1;
                })));
        dispatcher.execute("cmd b", new Object());
        assertThat(ref.get()).isEqualTo("second");
    }
}
