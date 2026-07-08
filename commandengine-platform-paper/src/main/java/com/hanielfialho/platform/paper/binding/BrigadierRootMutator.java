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
