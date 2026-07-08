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
package com.hanielfialho.platform.paper.binding;

import static org.assertj.core.api.Assertions.assertThat;

import com.hanielfialho.api.command.CommandMetadata;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.platform.paper.source.PaperCommandSource;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

final class PaperBrigadierPermissionPredicateTest {

    @Test
    void nodeVisibleWithPermission() {
        var binding = binding();
        var root = LiteralArgumentBuilder.<CommandSource>literal("root");
        var metadata = new CommandMetadata("root", List.of(), "", "test.perm", List.of());

        binding.register(root, metadata);

        assertThat(binding.getDispatcher().getRoot().getChild("root")).isNotNull();
    }

    @Test
    void nodeInvisibleWithoutPermission() {
        var node = LiteralArgumentBuilder.<CommandSource>literal("root")
                .requires(s -> s.hasPermission("admin.perm"))
                .build();

        var playerSource = playerSource(false);
        assertThat(node.canUse(playerSource)).isFalse();
    }

    @Test
    void consoleRespectsPredicate() {
        var node = LiteralArgumentBuilder.<CommandSource>literal("root")
                .requires(s -> s.hasPermission("console.only"))
                .build();

        var consoleSource = consoleSource();
        assertThat(node.canUse(consoleSource)).isTrue();
    }

    @Test
    void playerRespectsPredicate() {
        var node = LiteralArgumentBuilder.<CommandSource>literal("root")
                .requires(s -> s.hasPermission("player.only"))
                .build();

        var playerSource = playerSource(true);
        assertThat(node.canUse(playerSource)).isTrue();

        var playerSourceNoPerm = playerSource(false);
        assertThat(node.canUse(playerSourceNoPerm)).isFalse();
    }

    private static PaperBrigadierBinding binding() {
        var plugin = plugin();
        return new PaperBrigadierBinding(plugin, new SimpleCommandMap(server(), new LinkedHashMap<>()));
    }

    private static Plugin plugin() {
        return (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> "TestPlugin";
                    case "getLogger" -> Logger.getLogger(PaperBrigadierPermissionPredicateTest.class.getName());
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Server server() {
        return (Server) Proxy.newProxyInstance(
                Server.class.getClassLoader(),
                new Class<?>[] {Server.class},
                (proxy, method, args) -> defaultValue(method.getReturnType()));
    }

    private static CommandSource playerSource(boolean hasPerm) {
        var player = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[] {Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "hasPermission" -> hasPerm;
                    case "getName" -> "player";
                    case "isPermissionSet" -> hasPerm;
                    case "isOp" -> hasPerm;
                    default -> defaultValue(method.getReturnType());
                });
        return new PaperCommandSource(player);
    }

    private static CommandSource consoleSource() {
        var console = (ConsoleCommandSender) Proxy.newProxyInstance(
                ConsoleCommandSender.class.getClassLoader(),
                new Class<?>[] {ConsoleCommandSender.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "hasPermission" -> true;
                    case "getName" -> "Console";
                    default -> defaultValue(method.getReturnType());
                });
        return new PaperCommandSource(console);
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return null;
    }
}
