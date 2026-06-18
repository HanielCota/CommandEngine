package com.hanielfialho.api.command;

import com.hanielfialho.api.registry.BrigadierAdapter;
import org.jetbrains.annotations.NotNull;

/**
 * Interface implemented by adapters generated at compile time.
 */
public interface CommandAdapter {

    /**
     * Registers this command in the provided {@link BrigadierAdapter}.
     */
    void register(@NotNull BrigadierAdapter brigadier);

    /**
     * Removes this command from the dispatcher.
     * This is essential to avoid memory leaks during reloads.
     */
    void unregister(@NotNull BrigadierAdapter brigadier);

    /**
     * Static metadata for this command.
     */
    @NotNull
    CommandMetadata metadata();
}
