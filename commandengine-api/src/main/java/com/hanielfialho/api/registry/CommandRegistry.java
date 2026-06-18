package com.hanielfialho.api.registry;

import com.hanielfialho.api.command.CommandAdapter;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

/**
 * Command registry implemented by the runtime module.
 */
public interface CommandRegistry {

    void register(@NotNull CommandAdapter adapter);

    default void register(@NotNull Object owner, @NotNull CommandAdapter adapter) {
        register(adapter);
    }

    void unregister(@NotNull CommandAdapter adapter);

    void unregisterAll(@NotNull Object owner);

    @NotNull
    Collection<CommandAdapter> getAdapters();
}
