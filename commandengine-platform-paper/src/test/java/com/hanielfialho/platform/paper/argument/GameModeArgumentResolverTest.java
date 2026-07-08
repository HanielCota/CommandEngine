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

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.GameMode;
import org.junit.jupiter.api.Test;

final class GameModeArgumentResolverTest {

    @Test
    void survival() throws Exception {
        var resolver =
                new AbstractPaperArgumentResolver<GameMode>(GameMode.class, "gameMode", name -> GameMode.SURVIVAL) {};
        var captured = new AtomicReference<CommandContext<Object>>();
        var dispatcher = new CommandDispatcher<Object>();

        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.<Object, String>argument("mode", argumentType(resolver))
                        .executes(context -> {
                            captured.set(context);
                            return 1;
                        })));

        dispatcher.execute("root survival", new Object());

        assertThat(resolver.resolve(captured.get(), "mode")).isEqualTo(GameMode.SURVIVAL);
    }

    @Test
    void creative() throws Exception {
        var resolver =
                new AbstractPaperArgumentResolver<GameMode>(GameMode.class, "gameMode", name -> GameMode.CREATIVE) {};
        var captured = new AtomicReference<CommandContext<Object>>();
        var dispatcher = new CommandDispatcher<Object>();

        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.<Object, String>argument("mode", argumentType(resolver))
                        .executes(context -> {
                            captured.set(context);
                            return 1;
                        })));

        dispatcher.execute("root creative", new Object());

        assertThat(resolver.resolve(captured.get(), "mode")).isEqualTo(GameMode.CREATIVE);
    }

    @Test
    void adventure() throws Exception {
        var resolver =
                new AbstractPaperArgumentResolver<GameMode>(GameMode.class, "gameMode", name -> GameMode.ADVENTURE) {};
        var captured = new AtomicReference<CommandContext<Object>>();
        var dispatcher = new CommandDispatcher<Object>();

        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.<Object, String>argument("mode", argumentType(resolver))
                        .executes(context -> {
                            captured.set(context);
                            return 1;
                        })));

        dispatcher.execute("root adventure", new Object());

        assertThat(resolver.resolve(captured.get(), "mode")).isEqualTo(GameMode.ADVENTURE);
    }

    @Test
    void spectator() throws Exception {
        var resolver =
                new AbstractPaperArgumentResolver<GameMode>(GameMode.class, "gameMode", name -> GameMode.SPECTATOR) {};
        var captured = new AtomicReference<CommandContext<Object>>();
        var dispatcher = new CommandDispatcher<Object>();

        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.<Object, String>argument("mode", argumentType(resolver))
                        .executes(context -> {
                            captured.set(context);
                            return 1;
                        })));

        dispatcher.execute("root spectator", new Object());

        assertThat(resolver.resolve(captured.get(), "mode")).isEqualTo(GameMode.SPECTATOR);
    }

    @Test
    void caseInsensitive() throws Exception {
        var resolver =
                new AbstractPaperArgumentResolver<GameMode>(GameMode.class, "gameMode", name -> GameMode.SURVIVAL) {};
        var captured = new AtomicReference<CommandContext<Object>>();
        var dispatcher = new CommandDispatcher<Object>();

        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.<Object, String>argument("mode", argumentType(resolver))
                        .executes(context -> {
                            captured.set(context);
                            return 1;
                        })));

        dispatcher.execute("root SURVIVAL", new Object());

        assertThat(resolver.resolve(captured.get(), "mode")).isEqualTo(GameMode.SURVIVAL);
    }

    @Test
    void suggestion() {
        var resolver = new AbstractPaperArgumentResolver<GameMode>(GameMode.class, "gameMode", name -> null) {};
        assertThat(resolver.supportsDefault()).isTrue();
        assertThat(resolver.type()).isEqualTo(GameMode.class);
    }

    @SuppressWarnings("unchecked")
    private static ArgumentType<String> argumentType(AbstractPaperArgumentResolver<?> resolver) {
        return (ArgumentType<String>) resolver.argumentType();
    }
}
