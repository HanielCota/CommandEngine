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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

final class PaperBrigadierNodeTreeTest {

    @Test
    void rootNodeCreated() {
        var binding = binding();
        var metadata = new CommandMetadata("root", List.of(), "", "", List.of());

        binding.register(LiteralArgumentBuilder.<CommandSource>literal("root"), metadata);

        assertThat(binding.getDispatcher().getRoot().getChild("root")).isNotNull();
    }

    @Test
    void subcommandsBecomeChildNodes() {
        var binding = binding();
        var root = LiteralArgumentBuilder.<CommandSource>literal("root");
        root.then(LiteralArgumentBuilder.<CommandSource>literal("sub1"));
        root.then(LiteralArgumentBuilder.<CommandSource>literal("sub2"));
        var metadata = new CommandMetadata("root", List.of(), "", "", List.of());

        binding.register(root, metadata);

        var node = binding.getDispatcher().getRoot().getChild("root");
        assertThat(node).isNotNull();
        assertThat(node.getChild("sub1")).isNotNull();
        assertThat(node.getChild("sub2")).isNotNull();
    }

    @Test
    void argumentsBecomeArgumentNodes() {
        var binding = binding();
        var root = LiteralArgumentBuilder.<CommandSource>literal("root");
        root.then(RequiredArgumentBuilder.<CommandSource, String>argument("arg", StringArgumentType.word()));
        var metadata = new CommandMetadata("root", List.of(), "", "", List.of());

        binding.register(root, metadata);

        var node = binding.getDispatcher().getRoot().getChild("root");
        assertThat(node).isNotNull();
        assertThat(node.getChild("arg")).isNotNull();
    }

    @Test
    void aliasesCreateRedirectsOrEquivalent() {
        var binding = binding();
        var metadata = new CommandMetadata("root", List.of("r", "rt"), "", "", List.of());

        binding.register(LiteralArgumentBuilder.<CommandSource>literal("root"), metadata);

        assertThat(binding.getDispatcher().getRoot().getChild("root")).isNotNull();
        assertThat(binding.getDispatcher().getRoot().getChild("r")).isNull();
        assertThat(binding.getDispatcher().getRoot().getChild("rt")).isNull();
    }

    @Test
    void nodeOrderIsStable() {
        var binding = binding();
        var root = LiteralArgumentBuilder.<CommandSource>literal("root");
        root.then(LiteralArgumentBuilder.<CommandSource>literal("beta"));
        root.then(LiteralArgumentBuilder.<CommandSource>literal("alpha"));
        root.then(LiteralArgumentBuilder.<CommandSource>literal("gamma"));
        var metadata = new CommandMetadata("root", List.of(), "", "", List.of());

        binding.register(root, metadata);

        var children = binding.getDispatcher().getRoot().getChild("root").getChildren();
        var names = children.stream().map(n -> n.getName()).toList();
        assertThat(names).containsExactly("beta", "alpha", "gamma");
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
                    case "getLogger" -> Logger.getLogger(PaperBrigadierNodeTreeTest.class.getName());
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Server server() {
        return (Server) Proxy.newProxyInstance(
                Server.class.getClassLoader(),
                new Class<?>[] {Server.class},
                (proxy, method, args) -> defaultValue(method.getReturnType()));
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
