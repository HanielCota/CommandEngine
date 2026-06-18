package com.hanielfialho.runtime;

import com.hanielfialho.api.message.CommandMessages;
import java.time.Duration;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable runtime configuration for messages, async execution and built-in rate limiting.
 */
public record CommandEngineConfig(
        @NotNull CommandMessages messages,
        @NotNull Duration asyncTimeout,
        @NotNull Duration rateLimitWindow,
        int rateLimitMaxExecutions,
        long rateLimitMaximumSize) {

    public CommandEngineConfig {
        Objects.requireNonNull(messages, "messages");
        Objects.requireNonNull(asyncTimeout, "asyncTimeout");
        Objects.requireNonNull(rateLimitWindow, "rateLimitWindow");
        if (asyncTimeout.isZero() || asyncTimeout.isNegative()) {
            throw new IllegalArgumentException("asyncTimeout must be positive");
        }
        if (rateLimitWindow.isZero() || rateLimitWindow.isNegative()) {
            throw new IllegalArgumentException("rateLimitWindow must be positive");
        }
        if (rateLimitMaxExecutions < 1) {
            throw new IllegalArgumentException("rateLimitMaxExecutions must be positive");
        }
        if (rateLimitMaximumSize < 1) {
            throw new IllegalArgumentException("rateLimitMaximumSize must be positive");
        }
    }

    /**
     * Creates the default production-oriented runtime configuration.
     *
     * @return default runtime configuration
     */
    public static @NotNull CommandEngineConfig defaults() {
        return new CommandEngineConfig(
                CommandMessages.defaults(), Duration.ofSeconds(30), Duration.ofSeconds(2), 5, 10_000);
    }
}
