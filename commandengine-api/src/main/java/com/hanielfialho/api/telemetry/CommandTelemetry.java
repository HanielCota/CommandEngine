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
package com.hanielfialho.api.telemetry;

import com.hanielfialho.api.command.CommandPath;
import org.jetbrains.annotations.NotNull;

/**
 * SPI for command observability.
 */
public interface CommandTelemetry {

    CommandTelemetry NOOP = new CommandTelemetry() {
        @Override
        public void recordExecution(@NotNull CommandPath path, long nanos, boolean async) {
            // no-op: telemetry not enabled
        }

        @Override
        public void recordFailure(@NotNull CommandPath path, @NotNull String reason) {
            // no-op: telemetry not enabled
        }

        @Override
        public void recordFailure(@NotNull CommandPath path, @NotNull String reason, @NotNull Throwable throwable) {
            // no-op: telemetry not enabled
        }

        @Override
        public void recordSuggestion(@NotNull CommandPath path, long nanos, int suggestionCount) {
            // no-op: telemetry not enabled
        }
    };

    void recordExecution(@NotNull CommandPath path, long nanos, boolean async);

    void recordFailure(@NotNull CommandPath path, @NotNull String reason);

    default void recordFailure(@NotNull CommandPath path, @NotNull String reason, @NotNull Throwable throwable) {
        recordFailure(path, reason);
    }

    void recordSuggestion(@NotNull CommandPath path, long nanos, int suggestionCount);
}
