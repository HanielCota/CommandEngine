package com.hanielfialho.api.telemetry;

import com.hanielfialho.api.command.CommandPath;
import org.jetbrains.annotations.NotNull;

/**
 * SPI for command observability.
 */
public interface CommandTelemetry {

    CommandTelemetry NOOP = new CommandTelemetry() {
        @Override
        public void recordExecution(@NotNull CommandPath path, long nanos, boolean async) {}

        @Override
        public void recordFailure(@NotNull CommandPath path, @NotNull String reason) {}

        @Override
        public void recordFailure(@NotNull CommandPath path, @NotNull String reason, @NotNull Throwable throwable) {}

        @Override
        public void recordSuggestion(@NotNull CommandPath path, long nanos, int suggestionCount) {}
    };

    void recordExecution(@NotNull CommandPath path, long nanos, boolean async);

    void recordFailure(@NotNull CommandPath path, @NotNull String reason);

    default void recordFailure(@NotNull CommandPath path, @NotNull String reason, @NotNull Throwable throwable) {
        recordFailure(path, reason);
    }

    void recordSuggestion(@NotNull CommandPath path, long nanos, int suggestionCount);
}
