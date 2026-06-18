package com.hanielfialho.test.brigadier;

import com.hanielfialho.api.registry.BrigadierAdapter;
import com.hanielfialho.api.source.CommandSource;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public final class LocalBrigadierAdapter implements BrigadierAdapter {

    private final CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher<>();

    @Override
    public @NotNull CommandDispatcher<CommandSource> getDispatcher() {
        return dispatcher;
    }

    public @NotNull CommandDispatcher<CommandSource> dispatcher() {
        return dispatcher;
    }

    @Override
    public void unregister(@NotNull String name) {
        removeRootMapEntry("children", name);
        removeRootMapEntry("literals", name);
    }

    @SuppressWarnings("unchecked")
    private void removeRootMapEntry(String fieldName, String name) {
        try {
            var field = CommandNode.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            var nodes = (Map<String, ?>) field.get(dispatcher.getRoot());
            nodes.remove(name);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to remove Brigadier node " + name, exception);
        }
    }
}
