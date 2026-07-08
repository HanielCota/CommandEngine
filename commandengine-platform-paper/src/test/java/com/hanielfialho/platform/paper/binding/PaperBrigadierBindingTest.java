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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

final class PaperBrigadierBindingTest {

    @Test
    void unregisterRemovesBukkitCommandMapEntries() {
        var knownCommands = new LinkedHashMap<String, org.bukkit.command.Command>();
        var commandMap = new SimpleCommandMap(server(), knownCommands);
        var binding = new PaperBrigadierBinding(plugin(), commandMap);
        var metadata = new CommandMetadata("root", List.of("r"), "", "", List.of());

        binding.register(LiteralArgumentBuilder.<CommandSource>literal("root"), metadata);

        assertThat(commandMap.getKnownCommands()).containsKeys("root", "r", "testplugin:root", "testplugin:r");

        binding.unregister("root");

        assertThat(commandMap.getKnownCommands()).doesNotContainKeys("root", "r", "testplugin:root", "testplugin:r");
    }

    @Test
    void unregisterRemovesEntriesWhenKnownCommandsIsUnmodifiable() {
        var knownCommands = new LinkedHashMap<String, org.bukkit.command.Command>();
        var commandMap = new TestCommandMap(knownCommands);
        var binding = new PaperBrigadierBinding(plugin(), commandMap);
        var metadata = new CommandMetadata("root", List.of("r"), "", "", List.of());

        binding.register(LiteralArgumentBuilder.<CommandSource>literal("root"), metadata);

        assertThat(commandMap.getKnownCommands()).containsKeys("root", "r", "testplugin:root", "testplugin:r");

        commandMap.setKnownCommands(Collections.unmodifiableMap(new LinkedHashMap<>(knownCommands)));

        binding.unregister("root");

        assertThat(commandMap.getKnownCommands()).doesNotContainKeys("root", "r", "testplugin:root", "testplugin:r");
    }

    @Test
    void registerClaimsPluginYmlCommandEntriesFromSamePlugin() {
        var plugin = plugin();
        var existing = new ExistingPluginCommand("root", List.of("r"), plugin);
        var knownCommands = new LinkedHashMap<String, org.bukkit.command.Command>();
        knownCommands.put("root", existing);
        knownCommands.put("r", existing);
        knownCommands.put("testplugin:root", existing);
        knownCommands.put("testplugin:r", existing);
        var commandMap = new SimpleCommandMap(server(), knownCommands);
        var binding = new PaperBrigadierBinding(plugin, commandMap);
        var metadata = new CommandMetadata("root", List.of("r"), "", "", List.of());

        binding.register(LiteralArgumentBuilder.<CommandSource>literal("root"), metadata);

        assertThat(commandMap.getKnownCommands()).containsKeys("root", "r", "testplugin:root", "testplugin:r");
        assertThat(commandMap.getKnownCommands().get("root")).isNotSameAs(existing);
        assertThat(commandMap.getKnownCommands().get("r")).isNotSameAs(existing);
        assertThat(commandMap.getKnownCommands()).doesNotContainValue(existing);
    }

    @Test
    void unregisterNormalizesNameWhenRemovingDispatcherNode() {
        var knownCommands = new LinkedHashMap<String, org.bukkit.command.Command>();
        var commandMap = new SimpleCommandMap(server(), knownCommands);
        var binding = new PaperBrigadierBinding(plugin(), commandMap);
        var metadata = new CommandMetadata("root", List.of(), "", "", List.of());

        binding.register(LiteralArgumentBuilder.<CommandSource>literal("root"), metadata);

        assertThat(binding.getDispatcher().getRoot().getChild("root")).isNotNull();
        assertThat(commandMap.getKnownCommands()).containsKey("root");

        binding.unregister("ROOT");

        assertThat(commandMap.getKnownCommands()).doesNotContainKey("root");
        assertThat(binding.getDispatcher().getRoot().getChild("root")).isNull();
    }

    @Test
    void unregisterAllRemovesRegisteredAliasNodesFromDispatcher() {
        var knownCommands = new LinkedHashMap<String, org.bukkit.command.Command>();
        var commandMap = new SimpleCommandMap(server(), knownCommands);
        var binding = new PaperBrigadierBinding(plugin(), commandMap);
        var metadata = new CommandMetadata("root", List.of("r"), "", "", List.of());

        binding.register(LiteralArgumentBuilder.<CommandSource>literal("root"), metadata);
        binding.register(LiteralArgumentBuilder.<CommandSource>literal("r"), metadata);

        assertThat(binding.getDispatcher().getRoot().getChild("root")).isNotNull();
        assertThat(binding.getDispatcher().getRoot().getChild("r")).isNotNull();

        binding.unregisterAll();

        assertThat(binding.getDispatcher().getRoot().getChild("root")).isNull();
        assertThat(binding.getDispatcher().getRoot().getChild("r")).isNull();
    }

    private static Plugin plugin() {
        return (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class<?>[] {Plugin.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> "TestPlugin";
                    case "getLogger" -> Logger.getLogger(PaperBrigadierBindingTest.class.getName());
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
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0F;
        }
        if (type == double.class) {
            return 0D;
        }
        return null;
    }

    private static final class TestCommandMap extends SimpleCommandMap {

        private Map<String, Command> knownCommands;

        private TestCommandMap(Map<String, Command> knownCommands) {
            super(server(), knownCommands);
            this.knownCommands = knownCommands;
        }

        @Override
        public void clearCommands() {
            knownCommands.clear();
        }

        @Override
        public Command getCommand(String name) {
            return knownCommands.get(name);
        }

        @Override
        public Collection<Command> getCommands() {
            return List.copyOf(knownCommands.values());
        }

        @Override
        public Map<String, Command> getKnownCommands() {
            return knownCommands;
        }

        private void setKnownCommands(Map<String, Command> knownCommands) {
            this.knownCommands = knownCommands;
        }
    }

    private static final class ExistingPluginCommand extends Command implements PluginIdentifiableCommand {

        private final Plugin plugin;

        private ExistingPluginCommand(String name, List<String> aliases, Plugin plugin) {
            super(name);
            setAliases(aliases);
            this.plugin = plugin;
        }

        @Override
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            return true;
        }

        @Override
        public Plugin getPlugin() {
            return plugin;
        }
    }
}
