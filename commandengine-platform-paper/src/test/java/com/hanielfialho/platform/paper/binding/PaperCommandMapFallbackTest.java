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
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

final class PaperCommandMapFallbackTest {

    @Test
    void registrationViaCommandMap() {
        var knownCommands = new LinkedHashMap<String, Command>();
        var commandMap = new SimpleCommandMap(server(), knownCommands);
        var plugin = plugin();
        var binding = new PaperBrigadierBinding(plugin, commandMap);
        var metadata = new CommandMetadata("mycmd", List.of(), "", "", List.of());

        binding.register(LiteralArgumentBuilder.<CommandSource>literal("mycmd"), metadata);

        assertThat(knownCommands).containsKey("mycmd");
        assertThat(knownCommands).containsKey("testplugin:mycmd");
    }

    @Test
    void fallbackPrefixCorrect() {
        var knownCommands = new LinkedHashMap<String, Command>();
        var commandMap = new SimpleCommandMap(server(), knownCommands);
        var plugin = plugin();
        var binding = new PaperBrigadierBinding(plugin, commandMap);
        var metadata = new CommandMetadata("mycmd", List.of(), "", "", List.of());

        binding.register(LiteralArgumentBuilder.<CommandSource>literal("mycmd"), metadata);

        assertThat(knownCommands).containsKey("testplugin:mycmd");
        assertThat(knownCommands.get("testplugin:mycmd").getName()).isEqualTo("mycmd");
    }

    @Test
    void existingCommandConflict() {
        var plugin = plugin();
        var existing = new ExistingCommand("conflict");
        var knownCommands = new LinkedHashMap<String, Command>();
        knownCommands.put("conflict", existing);
        var commandMap = new SimpleCommandMap(server(), knownCommands);
        var binding = new PaperBrigadierBinding(plugin, commandMap);
        var metadata = new CommandMetadata("conflict", List.of(), "", "", List.of());

        binding.register(LiteralArgumentBuilder.<CommandSource>literal("conflict"), metadata);

        assertThat(knownCommands).containsKey("conflict");
        assertThat(knownCommands).containsKey("testplugin:conflict");
    }

    @Test
    void unregisterRemovesFromCommandMap() {
        var knownCommands = new LinkedHashMap<String, Command>();
        var commandMap = new SimpleCommandMap(server(), knownCommands);
        var plugin = plugin();
        var binding = new PaperBrigadierBinding(plugin, commandMap);
        var metadata = new CommandMetadata("mycmd", List.of(), "", "", List.of());

        binding.register(LiteralArgumentBuilder.<CommandSource>literal("mycmd"), metadata);

        assertThat(knownCommands).containsKey("mycmd");

        binding.unregister("mycmd");

        assertThat(knownCommands).doesNotContainKey("mycmd");
    }

    private static Plugin plugin() {
        return (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> "TestPlugin";
                    case "getLogger" -> Logger.getLogger(PaperCommandMapFallbackTest.class.getName());
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

    private static final class ExistingCommand extends Command {
        ExistingCommand(String name) {
            super(name);
        }

        @Override
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            return true;
        }
    }
}
