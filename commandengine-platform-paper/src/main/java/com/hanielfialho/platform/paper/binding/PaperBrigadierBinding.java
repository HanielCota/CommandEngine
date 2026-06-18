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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
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

        if (plugin == null || commandMap == null || !node.getName().equals(metadata.name())) {
            return node;
        }

        Command command = new PaperBridgeCommand(metadata, dispatcher, logger(), messages);
        commandMap.register(plugin.getName().toLowerCase(), command);
        registeredCommands.put(metadata.name(), command);
        return node;
    }

    @Override
    public void unregister(@NotNull String name) {
        Preconditions.checkNotNull(name, "name");
        removeRootNode(name);
        Command command = registeredCommands.remove(name);
        if (command == null || commandMap == null) {
            return;
        }
        command.unregister(commandMap);
        removeCommandMapEntries(command);
    }

    public void unregisterAll() {
        for (String name : List.copyOf(registeredCommands.keySet())) {
            unregister(name);
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
    private void removeCommandMapEntries(Command command) {
        try {
            Field knownCommandsField = findField(commandMap.getClass(), "knownCommands");
            knownCommandsField.setAccessible(true);
            Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);
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
