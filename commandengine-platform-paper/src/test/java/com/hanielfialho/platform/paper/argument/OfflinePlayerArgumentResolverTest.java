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
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;

final class OfflinePlayerArgumentResolverTest {

    @Test
    void playerOnline() throws Exception {
        var player = offlinePlayerProxy("haniel");
        var resolver = new AbstractPaperArgumentResolver<OfflinePlayer>(
                OfflinePlayer.class, "offlinePlayer", name -> player) {};
        var captured = new AtomicReference<CommandContext<Object>>();
        var dispatcher = new CommandDispatcher<Object>();

        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.<Object, String>argument("player", argumentType(resolver))
                        .executes(context -> {
                            captured.set(context);
                            return 1;
                        })));

        dispatcher.execute("root haniel", new Object());

        assertThat(resolver.resolve(captured.get(), "player")).isSameAs(player);
    }

    @Test
    void offlinePlayerKnown() throws Exception {
        var offline = offlinePlayerProxy("Notch");
        var resolver = new AbstractPaperArgumentResolver<OfflinePlayer>(
                OfflinePlayer.class, "offlinePlayer", name -> offline) {};
        var captured = new AtomicReference<CommandContext<Object>>();
        var dispatcher = new CommandDispatcher<Object>();

        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.<Object, String>argument("player", argumentType(resolver))
                        .executes(context -> {
                            captured.set(context);
                            return 1;
                        })));

        dispatcher.execute("root Notch", new Object());

        assertThat(resolver.resolve(captured.get(), "player")).isSameAs(offline);
    }

    @Test
    void playerNotFound() throws Exception {
        var resolver =
                new AbstractPaperArgumentResolver<OfflinePlayer>(OfflinePlayer.class, "offlinePlayer", name -> null) {};
        var captured = new AtomicReference<CommandContext<Object>>();
        var dispatcher = new CommandDispatcher<Object>();

        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.<Object, String>argument("player", argumentType(resolver))
                        .executes(context -> {
                            captured.set(context);
                            return 1;
                        })));

        dispatcher.execute("root nonexistent", new Object());

        assertThatThrownBy(() -> resolver.resolve(captured.get(), "player"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown offlinePlayer: nonexistent");
    }

    @Test
    void uuidInput() throws Exception {
        var uuid = UUID.randomUUID();
        var player = offlinePlayerProxy(uuid.toString());
        var resolver = new AbstractPaperArgumentResolver<OfflinePlayer>(
                OfflinePlayer.class, "offlinePlayer", name -> player) {};
        var captured = new AtomicReference<CommandContext<Object>>();
        var dispatcher = new CommandDispatcher<Object>();

        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.<Object, String>argument("player", argumentType(resolver))
                        .executes(context -> {
                            captured.set(context);
                            return 1;
                        })));

        dispatcher.execute("root " + uuid, new Object());

        assertThat(resolver.resolve(captured.get(), "player")).isSameAs(player);
    }

    @Test
    void nameSuggestions() {
        var resolver =
                new AbstractPaperArgumentResolver<OfflinePlayer>(OfflinePlayer.class, "offlinePlayer", name -> null) {};
        assertThat(resolver.supportsDefault()).isTrue();
        assertThat(resolver.type()).isEqualTo(OfflinePlayer.class);
    }

    @SuppressWarnings("unchecked")
    private static ArgumentType<String> argumentType(AbstractPaperArgumentResolver<?> resolver) {
        return (ArgumentType<String>) resolver.argumentType();
    }

    private static OfflinePlayer offlinePlayerProxy(String name) {
        return (OfflinePlayer) Proxy.newProxyInstance(
                OfflinePlayer.class.getClassLoader(), new Class<?>[] {OfflinePlayer.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getName")) return name;
                    return null;
                });
    }
}
