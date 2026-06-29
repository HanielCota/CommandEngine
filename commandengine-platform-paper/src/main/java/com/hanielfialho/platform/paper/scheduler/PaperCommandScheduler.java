package com.hanielfialho.platform.paper.scheduler;

import com.hanielfialho.api.scheduler.CommandScheduler;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class PaperCommandScheduler implements CommandScheduler {

    private static final Logger LOGGER = Logger.getLogger(PaperCommandScheduler.class.getName());

    private final Plugin plugin;

    public PaperCommandScheduler(@NotNull Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public void execute(@NotNull Runnable command) {
        Objects.requireNonNull(command, "command");
        if (!plugin.isEnabled()) {
            LOGGER.log(Level.FINE, "Dropping scheduler callback because plugin is disabled");
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            command.run();
            return;
        }
        try {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (plugin.isEnabled()) {
                    command.run();
                }
            });
        } catch (IllegalPluginAccessException exception) {
            LOGGER.log(Level.FINE, exception, () -> "Dropping scheduler callback because plugin was disabled");
        }
    }
}
