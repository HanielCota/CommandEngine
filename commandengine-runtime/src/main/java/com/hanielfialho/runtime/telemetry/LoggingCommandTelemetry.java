package com.hanielfialho.runtime.telemetry;

import com.hanielfialho.api.command.CommandPath;
import com.hanielfialho.api.telemetry.CommandTelemetry;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Command telemetry implementation backed by {@link java.util.logging.Logger}.
 */
public final class LoggingCommandTelemetry implements CommandTelemetry {

    private final Logger logger;

    /**
     * Creates telemetry that writes command events to the provided logger.
     *
     * @param logger destination logger
     */
    public LoggingCommandTelemetry(@NotNull Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public void recordExecution(@NotNull CommandPath path, long nanos, boolean async) {
        logger.log(
                Level.FINE,
                () -> "Command executed path="
                        + path
                        + " async="
                        + async
                        + " millis="
                        + TimeUnit.NANOSECONDS.toMillis(nanos));
    }

    @Override
    public void recordFailure(@NotNull CommandPath path, @NotNull String reason) {
        logger.log(Level.WARNING, () -> "Command failed path=" + path + " reason=" + reason);
    }

    @Override
    public void recordFailure(@NotNull CommandPath path, @NotNull String reason, @NotNull Throwable throwable) {
        logger.log(Level.WARNING, throwable, () -> "Command failed path=" + path + " reason=" + reason);
    }

    @Override
    public void recordSuggestion(@NotNull CommandPath path, long nanos, int suggestionCount) {
        logger.log(
                Level.FINER,
                () -> "Suggestions built path="
                        + path
                        + " count="
                        + suggestionCount
                        + " millis="
                        + TimeUnit.NANOSECONDS.toMillis(nanos));
    }
}
