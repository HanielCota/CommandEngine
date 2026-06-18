package com.hanielfialho.platform.paper.source;

import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.runtime.util.Preconditions;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * Adapter between Paper's CommandSender and CommandEngine's source abstraction.
 */
public final class PaperCommandSource implements CommandSource {

    private final CommandSender handle;

    public PaperCommandSource(@NotNull CommandSender handle) {
        this.handle = Preconditions.checkNotNull(handle, "handle");
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        Preconditions.checkNotNull(permission, "permission");
        return permission.isEmpty() || handle.hasPermission(permission);
    }

    @Override
    public @NotNull Object getHandle() {
        return handle;
    }

    @Override
    public void sendMessage(@NotNull String message) {
        handle.sendMessage(Preconditions.checkNotNull(message, "message"));
    }

    @Override
    public @NotNull String getName() {
        return handle.getName();
    }
}
