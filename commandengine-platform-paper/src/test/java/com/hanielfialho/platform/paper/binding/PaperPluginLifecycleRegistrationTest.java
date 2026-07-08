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
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

final class PaperPluginLifecycleRegistrationTest {

    @Test
    void registersOnEnable() {
        var knownCommands = new LinkedHashMap<String, org.bukkit.command.Command>();
        var commandMap = new SimpleCommandMap(server(), knownCommands);
        var plugin = plugin();
        var binding = new PaperBrigadierBinding(plugin, commandMap);
        var metadata = new CommandMetadata("mycmd", List.of(), "", "", List.of());

        binding.register(LiteralArgumentBuilder.<CommandSource>literal("mycmd"), metadata);

        assertThat(knownCommands).containsKey("mycmd");
        assertThat(binding.getDispatcher().getRoot().getChild("mycmd")).isNotNull();
    }

    @Test
    void removesOnDisable() {
        var knownCommands = new LinkedHashMap<String, org.bukkit.command.Command>();
        var commandMap = new SimpleCommandMap(server(), knownCommands);
        var plugin = plugin();
        var binding = new PaperBrigadierBinding(plugin, commandMap);
        var metadata = new CommandMetadata("mycmd", List.of(), "", "", List.of());

        binding.register(LiteralArgumentBuilder.<CommandSource>literal("mycmd"), metadata);
        assertThat(knownCommands).containsKey("mycmd");

        binding.unregisterAll();

        assertThat(knownCommands).doesNotContainKey("mycmd");
        assertThat(binding.getDispatcher().getRoot().getChild("mycmd")).isNull();
    }

    @Test
    void reloadDoesNotDuplicate() {
        var knownCommands = new LinkedHashMap<String, org.bukkit.command.Command>();
        var commandMap = new SimpleCommandMap(server(), knownCommands);
        var plugin = plugin();
        var binding = new PaperBrigadierBinding(plugin, commandMap);
        var metadata = new CommandMetadata("mycmd", List.of(), "", "", List.of());

        binding.register(LiteralArgumentBuilder.<CommandSource>literal("mycmd"), metadata);
        binding.register(LiteralArgumentBuilder.<CommandSource>literal("mycmd"), metadata);
        binding.register(LiteralArgumentBuilder.<CommandSource>literal("mycmd"), metadata);

        var entries = knownCommands.entrySet().stream()
                .filter(e -> e.getKey().equals("mycmd") || e.getKey().equals("testplugin:mycmd"))
                .count();
        assertThat(entries).isEqualTo(2L);
    }

    @Test
    void disableDuringExecution() {
        var knownCommands = new LinkedHashMap<String, org.bukkit.command.Command>();
        var commandMap = new SimpleCommandMap(server(), knownCommands);
        var plugin = plugin();
        var binding = new PaperBrigadierBinding(plugin, commandMap);
        var metadata = new CommandMetadata("mycmd", List.of(), "", "", List.of());

        binding.register(LiteralArgumentBuilder.<CommandSource>literal("mycmd"), metadata);
        assertThat(knownCommands).containsKey("mycmd");

        binding.unregister("mycmd");

        assertThat(knownCommands).doesNotContainKey("mycmd");
        assertThat(binding.getDispatcher().getRoot().getChild("mycmd")).isNull();

        binding.register(LiteralArgumentBuilder.<CommandSource>literal("mycmd"), metadata);

        assertThat(knownCommands).containsKey("mycmd");
        assertThat(binding.getDispatcher().getRoot().getChild("mycmd")).isNotNull();
    }

    private static Plugin plugin() {
        return (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> "TestPlugin";
                    case "getLogger" -> Logger.getLogger(PaperPluginLifecycleRegistrationTest.class.getName());
                    case "isEnabled" -> true;
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
