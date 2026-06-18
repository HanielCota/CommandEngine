package com.hanielfialho.platform.paper.config;

import com.hanielfialho.api.message.CommandMessages;
import com.hanielfialho.runtime.CommandEngineConfig;
import java.time.Duration;
import java.util.Objects;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Loads CommandEngine runtime settings from a Paper plugin configuration.
 */
public final class PaperCommandEngineConfigLoader {

    private static final String PREFIX = "commandengine.";

    private PaperCommandEngineConfigLoader() {}

    /**
     * Loads settings from {@link Plugin#getConfig()}.
     *
     * @param plugin source plugin
     * @return runtime configuration
     */
    public static @NotNull CommandEngineConfig load(@NotNull Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        return load(plugin.getConfig());
    }

    /**
     * Loads settings from a Bukkit file configuration.
     *
     * @param config source configuration
     * @return runtime configuration
     */
    public static @NotNull CommandEngineConfig load(@NotNull FileConfiguration config) {
        Objects.requireNonNull(config, "config");
        var defaults = CommandEngineConfig.defaults();
        return new CommandEngineConfig(
                messages(config, defaults.messages()),
                duration(config, "async-timeout-millis", defaults.asyncTimeout()),
                duration(config, "rate-limit.window-millis", defaults.rateLimitWindow()),
                config.getInt(PREFIX + "rate-limit.max-executions", defaults.rateLimitMaxExecutions()),
                config.getLong(PREFIX + "rate-limit.maximum-size", defaults.rateLimitMaximumSize()));
    }

    private static CommandMessages messages(FileConfiguration config, CommandMessages defaults) {
        return new CommandMessages(
                string(config, "messages.internal-error", defaults.internalError()),
                string(config, "messages.invalid-sender", defaults.invalidSender()),
                string(config, "messages.invalid-syntax", defaults.invalidSyntax()),
                string(config, "messages.no-permission", defaults.noPermission()),
                string(config, "messages.rate-limited", defaults.rateLimited()));
    }

    private static Duration duration(FileConfiguration config, String path, Duration defaults) {
        return Duration.ofMillis(config.getLong(PREFIX + path, defaults.toMillis()));
    }

    private static String string(FileConfiguration config, String path, String defaults) {
        return Objects.requireNonNull(config.getString(PREFIX + path, defaults), path);
    }
}
