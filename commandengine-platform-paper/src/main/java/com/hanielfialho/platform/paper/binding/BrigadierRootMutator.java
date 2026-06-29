package com.hanielfialho.platform.paper.binding;

import com.hanielfialho.api.source.CommandSource;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import java.util.Map;
import java.util.function.BiConsumer;
import org.jetbrains.annotations.NotNull;

/**
 * Isolates the only reflective Brigadier mutation needed by the Paper binding.
 */
final class BrigadierRootMutator {

    private BrigadierRootMutator() {}

    static void removeRootNode(
            @NotNull CommandDispatcher<CommandSource> dispatcher,
            @NotNull String name,
            @NotNull BiConsumer<String, ReflectiveOperationException> failureHandler) {
        removeRootMapEntry(dispatcher, "children", name, failureHandler);
        removeRootMapEntry(dispatcher, "literals", name, failureHandler);
    }

    @SuppressWarnings({"unchecked", "java:S2201"})
    private static void removeRootMapEntry(
            CommandDispatcher<CommandSource> dispatcher,
            String fieldName,
            String name,
            BiConsumer<String, ReflectiveOperationException> failureHandler) {
        try {
            var field = CommandNode.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            var nodes = (Map<String, ?>) field.get(dispatcher.getRoot());
            nodes.remove(name);
        } catch (ReflectiveOperationException exception) {
            failureHandler.accept(fieldName, exception);
        }
    }
}
