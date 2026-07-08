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

final class CommandPathMatchingTest {

    @Test
    void rootPath() throws Exception {
        var captured = new AtomicReference<String>();
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root").executes(ctx -> {
            captured.set("executed");
            return 1;
        }));
        dispatcher.execute("root", new Object());
        assertThat(captured.get()).isEqualTo("executed");
    }

    @Test
    void pathWithSubcommand() throws Exception {
        var captured = new AtomicReference<String>();
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("parent")
                .then(LiteralArgumentBuilder.<Object>literal("child").executes(ctx -> {
                    captured.set("child-executed");
                    return 1;
                })));
        dispatcher.execute("parent child", new Object());
        assertThat(captured.get()).isEqualTo("child-executed");
    }

    @Test
    void pathWithMultipleLevels() throws Exception {
        var captured = new AtomicReference<String>();
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("a")
                .then(LiteralArgumentBuilder.<Object>literal("b")
                        .then(LiteralArgumentBuilder.<Object>literal("c").executes(ctx -> {
                            captured.set("deep");
                            return 1;
                        }))));
        dispatcher.execute("a b c", new Object());
        assertThat(captured.get()).isEqualTo("deep");
    }

    @Test
    void pathCaseSensitive() throws Exception {
        var captured = new AtomicReference<String>();
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("Command").executes(ctx -> {
            captured.set("upper");
            return 1;
        }));
        dispatcher.execute("Command", new Object());
        assertThat(captured.get()).isEqualTo("upper");
    }

    @Test
    void pathWithAliasAtIntermediateLevel() throws Exception {
        var captured = new AtomicReference<String>();
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("server")
                .then(LiteralArgumentBuilder.<Object>literal("msg")
                        .then(LiteralArgumentBuilder.<Object>literal("send").executes(ctx -> {
                            captured.set("sent");
                            return 1;
                        })))
                .then(LiteralArgumentBuilder.<Object>literal("tell")
                        .then(LiteralArgumentBuilder.<Object>literal("send").executes(ctx -> {
                            captured.set("alias-sent");
                            return 1;
                        }))));
        dispatcher.execute("server msg send", new Object());
        assertThat(captured.get()).isEqualTo("sent");
        dispatcher.execute("server tell send", new Object());
        assertThat(captured.get()).isEqualTo("alias-sent");
    }
}
