package com.hanielfialho.api.source;

import org.jetbrains.annotations.NotNull;

/**
 * Platform-agnostic abstraction for a command source.
 */
public interface CommandSource extends PermissionHolder {

    /**
     * Returns whether this source has the given permission.
     */
    boolean hasPermission(@NotNull String permission);

    /**
     * Returns the native platform handle, for example a Bukkit {@code Player}.
     * Use this carefully and prefer abstractions when possible.
     */
    @NotNull
    Object getHandle();

    /**
     * Sends a plain text message to this source.
     */
    void sendMessage(@NotNull String message);

    /**
     * Returns this source's display or identifier name.
     */
    @NotNull
    String getName();
}
