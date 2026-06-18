package com.hanielfialho.platform.paper.binding;

import com.hanielfialho.api.command.CommandMetadata;
import com.hanielfialho.api.message.CommandMessages;
import com.hanielfialho.api.registry.BrigadierAdapter;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.platform.paper.command.PaperBridgeCommand;
import com.hanielfialho.runtime.util.Preconditions;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Paper binding that dispatches generated commands through a local Brigadier dispatcher.
 */
public final class PaperBrigadierBinding implements BrigadierAdapter {

    private final CommandDispatcher<CommandSource> dispatcher;

    @Nullable
    private final Plugin plugin;

    @Nullable
    private final CommandMap commandMap;

    private final CommandMessages messages;

    private final Map<String, Command> registeredCommands = new ConcurrentHashMap<>();

    public PaperBrigadierBinding(@NotNull CommandDispatcher<CommandSource> dispatcher) {
        this(null, null, dispatcher, CommandMessages.defaults());
    }

    public PaperBrigadierBinding(@NotNull Plugin plugin, @NotNull CommandMap commandMap) {
        this(plugin, commandMap, CommandMessages.defaults());
    }

    public PaperBrigadierBinding(
            @NotNull Plugin plugin, @NotNull CommandMap commandMap, @NotNull CommandMessages messages) {
        this(plugin, commandMap, new CommandDispatcher<>(), messages);
    }

    private PaperBrigadierBinding(
            @Nullable Plugin plugin,
            @Nullable CommandMap commandMap,
            @NotNull CommandDispatcher<CommandSource> dispatcher,
            @NotNull CommandMessages messages) {
        this.plugin = plugin;
        this.commandMap = commandMap;
        this.dispatcher = Preconditions.checkNotNull(dispatcher, "dispatcher");
        this.messages = Preconditions.checkNotNull(messages, "messages");
    }

    @Override
    public @NotNull CommandDispatcher<CommandSource> getDispatcher() {
        return dispatcher;
    }

    @Override
    public @NotNull LiteralCommandNode<CommandSource> register(
            @NotNull LiteralArgumentBuilder<CommandSource> root, @NotNull CommandMetadata metadata) {
        return register(Preconditions.checkNotNull(root, "root").build(), metadata);
    }

    @Override
    public @NotNull LiteralCommandNode<CommandSource> register(
            @NotNull LiteralCommandNode<CommandSource> node, @NotNull CommandMetadata metadata) {
        Preconditions.checkNotNull(node, "node");
        Preconditions.checkNotNull(metadata, "metadata");
        dispatcher.getRoot().addChild(node);

        Plugin currentPlugin = plugin;
        CommandMap currentCommandMap = commandMap;
        if (currentPlugin == null || currentCommandMap == null || !node.getName().equals(metadata.name())) {
            return node;
        }

        removeClaimedPluginCommands(currentCommandMap, currentPlugin, metadata);
        Command command = new PaperBridgeCommand(metadata, dispatcher, logger(), messages);
        currentCommandMap.register(currentPlugin.getName().toLowerCase(Locale.ROOT), command);
        registeredCommands.put(metadata.name(), command);
        return node;
    }

    @Override
    public void unregister(@NotNull String name) {
        Preconditions.checkNotNull(name, "name");
        removeRootNode(name);
        Command command = registeredCommands.get(name);
        if (command == null) {
            String normalized = normalize(name);
            for (Map.Entry<String, Command> entry : registeredCommands.entrySet()) {
                if (normalize(entry.getValue().getName()).equals(normalized)) {
                    command = entry.getValue();
                    break;
                }
                List<String> aliases = entry.getValue().getAliases();
                if (aliases != null && aliases.stream().map(this::normalize).anyMatch(normalized::equals)) {
                    command = entry.getValue();
                    break;
                }
            }
        }
        registeredCommands.remove(name);
        CommandMap currentCommandMap = commandMap;
        if (command == null || currentCommandMap == null) {
            return;
        }
        command.unregister(currentCommandMap);
        removeCommandMapEntries(currentCommandMap, command);
    }

    public void unregisterAll() {
        RuntimeException failure = null;
        for (String name : List.copyOf(registeredCommands.keySet())) {
            try {
                unregister(name);
            } catch (RuntimeException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private void removeRootNode(String name) {
        BrigadierRootMutator.removeRootNode(
                dispatcher,
                name,
                (fieldName, exception) -> log(() -> "Unable to remove Brigadier node " + name, exception));
    }

    private void log(Supplier<String> message, Throwable throwable) {
        if (plugin == null) {
            return;
        }
        plugin.getLogger().log(Level.FINE, throwable, message);
    }

    private Logger logger() {
        return plugin == null ? Logger.getLogger(PaperBrigadierBinding.class.getName()) : plugin.getLogger();
    }

    @SuppressWarnings("unchecked")
    private void removeCommandMapEntries(CommandMap currentCommandMap, Command command) {
        try {
            Map<String, Command> knownCommands = knownCommands(currentCommandMap);
            for (Iterator<Map.Entry<String, Command>> iterator =
                            knownCommands.entrySet().iterator();
                    iterator.hasNext(); ) {
                if (iterator.next().getValue() == command) {
                    iterator.remove();
                }
            }
        } catch (ReflectiveOperationException exception) {
            log(() -> "Unable to remove command map entries for " + command.getName(), exception);
        }
    }

    private void removeClaimedPluginCommands(
            CommandMap currentCommandMap, Plugin currentPlugin, CommandMetadata metadata) {
        try {
            Map<String, Command> knownCommands = knownCommands(currentCommandMap);
            Set<String> labels = commandLabels(metadata);
            Set<Command> claimedCommands = Collections.newSetFromMap(new IdentityHashMap<>());
            for (Command command : knownCommands.values()) {
                if (isOwnedByPlugin(command, currentPlugin) && usesAnyLabel(command, labels)) {
                    claimedCommands.add(command);
                }
            }
            if (claimedCommands.isEmpty()) {
                return;
            }
            for (Command claimed : claimedCommands) {
                claimed.unregister(currentCommandMap);
            }
            for (Iterator<Map.Entry<String, Command>> iterator =
                            knownCommands.entrySet().iterator();
                    iterator.hasNext(); ) {
                if (claimedCommands.contains(iterator.next().getValue())) {
                    iterator.remove();
                }
            }
        } catch (ReflectiveOperationException exception) {
            log(() -> "Unable to claim Bukkit command map entries for " + metadata.name(), exception);
        }
    }

    private boolean isOwnedByPlugin(Command command, Plugin currentPlugin) {
        return command instanceof PluginIdentifiableCommand identifiable && identifiable.getPlugin() == currentPlugin;
    }

    private boolean usesAnyLabel(Command command, Set<String> labels) {
        if (labels.contains(normalize(command.getName()))) {
            return true;
        }
        List<String> aliases = command.getAliases();
        if (aliases == null) {
            return false;
        }
        for (String alias : aliases) {
            if (labels.contains(normalize(alias))) {
                return true;
            }
        }
        return false;
    }

    private Set<String> commandLabels(CommandMetadata metadata) {
        Set<String> labels = new HashSet<>();
        labels.add(normalize(metadata.name()));
        for (String alias : metadata.aliases()) {
            labels.add(normalize(alias));
        }
        return labels;
    }

    private String normalize(String label) {
        return label.toLowerCase(Locale.ROOT);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Command> knownCommands(CommandMap currentCommandMap) throws ReflectiveOperationException {
        Field knownCommandsField = findField(currentCommandMap.getClass(), "knownCommands");
        knownCommandsField.setAccessible(true);
        return (Map<String, Command>) knownCommandsField.get(currentCommandMap);
    }

    private Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException exception) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
