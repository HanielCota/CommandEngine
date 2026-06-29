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
                positiveDuration(config, "async-timeout-millis", defaults.asyncTimeout()),
                positiveDuration(config, "suggestion-timeout-millis", defaults.suggestionTimeout()),
                positiveDuration(config, "rate-limit.window-millis", defaults.rateLimitWindow()),
                positiveInt(config, "rate-limit.max-executions", defaults.rateLimitMaxExecutions()),
                positiveLong(config, "rate-limit.maximum-size", defaults.rateLimitMaximumSize()));
    }

    private static CommandMessages messages(FileConfiguration config, CommandMessages defaults) {
        return new CommandMessages(
                string(config, "messages.internal-error", defaults.internalError()),
                string(config, "messages.invalid-sender", defaults.invalidSender()),
                string(config, "messages.invalid-syntax", defaults.invalidSyntax()),
                string(config, "messages.no-permission", defaults.noPermission()),
                string(config, "messages.rate-limited", defaults.rateLimited()));
    }

    private static Duration positiveDuration(FileConfiguration config, String path, Duration defaults) {
        var value = config.get(PREFIX + path);
        if (value == null) {
            return defaults;
        }
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("Expected a number at " + PREFIX + path + " but got "
                    + value.getClass().getSimpleName());
        }
        long millis = number.longValue();
        if (millis <= 0) {
            throw new IllegalArgumentException(PREFIX + path + " must be a positive number (got " + millis + ")");
        }
        return Duration.ofMillis(millis);
    }

    private static int positiveInt(FileConfiguration config, String path, int defaults) {
        var value = config.get(PREFIX + path);
        if (value == null) {
            return defaults;
        }
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("Expected a number at " + PREFIX + path + " but got "
                    + value.getClass().getSimpleName());
        }
        int result = number.intValue();
        if (result < 1) {
            throw new IllegalArgumentException(PREFIX + path + " must be >= 1 (got " + result + ")");
        }
        return result;
    }

    private static long positiveLong(FileConfiguration config, String path, long defaults) {
        var value = config.get(PREFIX + path);
        if (value == null) {
            return defaults;
        }
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("Expected a number at " + PREFIX + path + " but got "
                    + value.getClass().getSimpleName());
        }
        long result = number.longValue();
        if (result < 1) {
            throw new IllegalArgumentException(PREFIX + path + " must be >= 1 (got " + result + ")");
        }
        return result;
    }

    private static String string(FileConfiguration config, String path, String defaults) {
        var value = config.get(PREFIX + path);
        if (value == null) {
            return defaults;
        }
        if (!(value instanceof String string)) {
            throw new IllegalArgumentException("Expected a string at " + PREFIX + path + " but got "
                    + value.getClass().getSimpleName());
        }
        return string;
    }

    @SuppressWarnings("unused")
    private static Duration duration(FileConfiguration config, String path, Duration defaults) {
        return positiveDuration(config, path, defaults);
    }
}
