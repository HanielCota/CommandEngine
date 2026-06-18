package com.hanielfialho.platform.paper.listener;

import com.hanielfialho.runtime.util.Preconditions;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class PluginDisableListener implements Listener {

    private final Plugin plugin;
    private final Runnable callback;

    public PluginDisableListener(@NotNull Plugin plugin, @NotNull Runnable callback) {
        this.plugin = Preconditions.checkNotNull(plugin, "plugin");
        this.callback = Preconditions.checkNotNull(callback, "callback");
    }

    @EventHandler
    public void onPluginDisable(@NotNull PluginDisableEvent event) {
        if (!event.getPlugin().equals(plugin)) {
            return;
        }
        callback.run();
    }
}
