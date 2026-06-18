package com.hanielfialho.runtime;

import com.hanielfialho.api.source.CommandSource;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import java.util.Map;

final class TestBrigadierRootMutator {

    private TestBrigadierRootMutator() {}

    static void remove(CommandDispatcher<CommandSource> dispatcher, String name) {
        removeRootMapEntry(dispatcher, "children", name);
        removeRootMapEntry(dispatcher, "literals", name);
    }

    @SuppressWarnings("unchecked")
    private static void removeRootMapEntry(CommandDispatcher<CommandSource> dispatcher, String fieldName, String name) {
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
