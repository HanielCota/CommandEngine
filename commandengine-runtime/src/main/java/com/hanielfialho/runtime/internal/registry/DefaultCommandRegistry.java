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
        adapters.computeIfPresent(name, (ignored, current) -> current == adapter ? null : current);
        ownerIndex.values().forEach(names -> names.remove(name));
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
