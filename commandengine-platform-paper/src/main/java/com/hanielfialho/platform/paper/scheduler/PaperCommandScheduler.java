package com.hanielfialho.platform.paper.scheduler;

import com.hanielfialho.api.scheduler.CommandScheduler;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class PaperCommandScheduler implements CommandScheduler {

    private final Plugin plugin;

    public PaperCommandScheduler(@NotNull Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public void execute(@NotNull Runnable command) {
        Objects.requireNonNull(command, "command");
        if (!plugin.isEnabled()) {
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            command.run();
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (plugin.isEnabled()) {
                command.run();
            }
        });
    }
}
