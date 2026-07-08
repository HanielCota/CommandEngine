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
package com.hanielfialho.runtime.internal.registry;

import com.hanielfialho.api.command.CommandAdapter;
import com.hanielfialho.api.registry.CommandRegistry;
import com.hanielfialho.runtime.util.Preconditions;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("java:S2201")
public final class DefaultCommandRegistry implements CommandRegistry {

    private final Map<String, CommandAdapter> adapters = new ConcurrentHashMap<>();
    private final Map<Object, Set<String>> ownerIndex = new ConcurrentHashMap<>();

    @Override
    public void register(@NotNull Object owner, @NotNull CommandAdapter adapter) {
        Preconditions.checkNotNull(owner, "owner");
        Preconditions.checkNotNull(adapter, "adapter");
        var name = adapter.metadata().name();
        var previous = adapters.putIfAbsent(name, adapter);
        if (previous != null) {
            throw new IllegalStateException("Command already registered: " + name);
        }
        ownerIndex
                .computeIfAbsent(owner, ignored -> ConcurrentHashMap.newKeySet())
                .add(name);
    }

    @Override
    public void unregister(@NotNull CommandAdapter adapter) {
        Preconditions.checkNotNull(adapter, "adapter");
        var name = adapter.metadata().name();
        var previous = adapters.computeIfPresent(name, (ignored, current) -> current == adapter ? null : current);
        if (previous == adapter) {
            ownerIndex.values().forEach(names -> names.remove(name));
        }
    }

    @Override
    public void unregisterAll(@NotNull Object owner) {
        Preconditions.checkNotNull(owner, "owner");
        var names = ownerIndex.remove(owner);
        if (names == null) {
            return;
        }
        names.forEach(adapters::remove);
    }

    @Override
    public @NotNull Collection<CommandAdapter> getAdapters() {
        return List.copyOf(adapters.values());
    }

    public @NotNull Collection<CommandAdapter> getAdapters(@NotNull Object owner) {
        Preconditions.checkNotNull(owner, "owner");
        var names = ownerIndex.get(owner);
        if (names == null || names.isEmpty()) {
            return Set.of();
        }

        var ownedAdapters = new HashSet<CommandAdapter>();
        for (var name : names) {
            var adapter = adapters.get(name);
            if (adapter != null) {
                ownedAdapters.add(adapter);
            }
        }
        return Set.copyOf(ownedAdapters);
    }
}
