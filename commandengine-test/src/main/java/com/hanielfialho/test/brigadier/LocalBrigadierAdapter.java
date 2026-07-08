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
package com.hanielfialho.test.brigadier;

import com.hanielfialho.api.registry.BrigadierAdapter;
import com.hanielfialho.api.source.CommandSource;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Local {@link BrigadierAdapter} used by integration tests.
 *
 * <p>Brigadier does not expose a public API to remove nodes from a dispatcher's root. The
 * {@link #unregister(String)} implementation uses reflection on {@link CommandNode} private fields
 * to delete the entries from the internal {@code children} and {@code literals} maps. This is
 * isolated in {@link #removeRootMapEntry(String, String)} and is acceptable only because this class
 * lives in the test harness, not in production code.
 */
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

    /**
     * Removes an entry from a private map inside the dispatcher's root node. This is the only place
     * in the project where test code reaches into Brigadier internals; it is intentionally confined
     * here rather than duplicated across tests.
     */
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
