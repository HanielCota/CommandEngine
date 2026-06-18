package com.hanielfialho.api.registry;

import com.hanielfialho.api.command.CommandAdapter;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

/**
 * Command registry implemented by the runtime module.
 */
public interface CommandRegistry {

    /**
     * Registers an adapter scoped to the given owner.
     */
    void register(@NotNull Object owner, @NotNull CommandAdapter adapter);

    /**
     * Registers an adapter using the registry itself as owner.
     */
    default void register(@NotNull CommandAdapter adapter) {
        register(this, adapter);
    }

    void unregister(@NotNull CommandAdapter adapter);

    void unregisterAll(@NotNull Object owner);

    @NotNull
    Collection<CommandAdapter> getAdapters();
}
