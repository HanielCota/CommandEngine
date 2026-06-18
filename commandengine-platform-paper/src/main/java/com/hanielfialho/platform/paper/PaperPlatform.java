package com.hanielfialho.platform.paper;

import com.hanielfialho.api.argument.ArgumentResolverRegistry;
import com.hanielfialho.api.command.CommandAdapter;
import com.hanielfialho.api.executor.CommandExecutor;
import com.hanielfialho.api.message.CommandMessages;
import com.hanielfialho.api.rate.CommandRateLimiter;
import com.hanielfialho.api.registry.BrigadierAdapter;
import com.hanielfialho.api.registry.CommandRegistry;
import com.hanielfialho.api.scheduler.CommandScheduler;
import com.hanielfialho.platform.paper.argument.LocationArgumentResolver;
import com.hanielfialho.platform.paper.argument.MaterialArgumentResolver;
import com.hanielfialho.platform.paper.argument.PlayerArgumentResolver;
import com.hanielfialho.platform.paper.argument.WorldArgumentResolver;
import com.hanielfialho.platform.paper.binding.PaperBrigadierBinding;
import com.hanielfialho.platform.paper.config.PaperCommandEngineConfigLoader;
import com.hanielfialho.platform.paper.listener.PluginDisableListener;
import com.hanielfialho.platform.paper.scheduler.PaperCommandScheduler;
import com.hanielfialho.runtime.CommandEngine;
import com.hanielfialho.runtime.CommandEngineConfig;
import com.hanielfialho.runtime.util.Preconditions;
import java.util.List;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Platform entry point for creating a CommandEngine in Paper plugins.
 */
public final class PaperPlatform implements CommandEngine.Platform, AutoCloseable {

    private final Plugin plugin;
    private final CommandRegistry registry;
    private final PaperBrigadierBinding brigadier;
    private final CommandExecutor executor;
    private final ArgumentResolverRegistry argumentResolvers;
    private final CommandScheduler scheduler;
    private final CommandMessages messages;
    private final CommandRateLimiter rateLimiter;

    private PaperPlatform(@NotNull Plugin plugin, @NotNull CommandEngineConfig config) {
        this.plugin = Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(config, "config");
        this.registry = CommandEngine.defaultRegistry();
        this.messages = config.messages();
        this.rateLimiter = CommandEngine.configuredRateLimiter(config);
        this.scheduler = new PaperCommandScheduler(plugin);
        var server = plugin.getServer();
        this.brigadier = new PaperBrigadierBinding(plugin, server.getCommandMap(), messages);
        this.executor = CommandEngine.virtualThreadExecutor(messages, config.asyncTimeout());
        this.argumentResolvers = CommandEngine.defaultArgumentResolverRegistry()
                .register(new PlayerArgumentResolver())
                .register(new WorldArgumentResolver())
                .register(new LocationArgumentResolver())
                .register(new MaterialArgumentResolver());
    }

    public static @NotNull PaperPlatform create(@NotNull Plugin plugin) {
        Preconditions.checkNotNull(plugin, "plugin");
        return create(plugin, PaperCommandEngineConfigLoader.load(plugin));
    }

    public static @NotNull PaperPlatform create(@NotNull Plugin plugin, @NotNull CommandEngineConfig config) {
        Preconditions.checkNotNull(plugin, "plugin");
        var platform = new PaperPlatform(plugin, config);
        plugin.getServer()
                .getPluginManager()
                .registerEvents(new PluginDisableListener(plugin, platform::close), plugin);
        return platform;
    }

    @Override
    public @NotNull CommandRegistry registry() {
        return registry;
    }

    @Override
    public @NotNull BrigadierAdapter brigadier() {
        return brigadier;
    }

    @Override
    public @NotNull CommandExecutor executor() {
        return executor;
    }

    @Override
    public @NotNull ArgumentResolverRegistry argumentResolvers() {
        return argumentResolvers;
    }

    @Override
    public @NotNull CommandScheduler scheduler() {
        return scheduler;
    }

    @Override
    public @NotNull CommandMessages messages() {
        return messages;
    }

    @Override
    public @NotNull CommandRateLimiter rateLimiter() {
        return rateLimiter;
    }

    @Override
    public @NotNull Object owner() {
        return plugin;
    }

    public void unregisterAll() {
        for (CommandAdapter adapter : List.copyOf(registry.getAdapters())) {
            adapter.unregister(brigadier);
        }
        registry.unregisterAll(plugin);
        brigadier.unregisterAll();
    }

    @Override
    public void close() {
        unregisterAll();
        if (executor instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to close command executor", exception);
            }
        }
    }
}
