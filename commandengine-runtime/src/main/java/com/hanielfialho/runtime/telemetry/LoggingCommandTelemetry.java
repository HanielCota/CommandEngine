/*
 * Copyright (c) 2026 Haniel Fialho
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
