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

import static org.assertj.core.api.Assertions.assertThat;

import com.hanielfialho.api.command.CommandPath;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class LoggingCommandTelemetryTest {

    private final List<LogRecord> records = new ArrayList<>();
    private LoggingCommandTelemetry telemetry;
    private static final CommandPath PATH = new CommandPath("test", "cmd");

    @BeforeEach
    void setUp() {
        records.clear();
        var logger = Logger.getLogger("test.telemetry");
        logger.setLevel(Level.ALL);
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        });
        telemetry = new LoggingCommandTelemetry(logger);
    }

    @Test
    void recordExecutionLogsAtFineLevel() {
        telemetry.recordExecution(PATH, 1_000_000L, false);

        assertThat(records).hasSize(1);
        var record = records.get(0);
        assertThat(record.getLevel()).isEqualTo(Level.FINE);
        assertThat(record.getMessage()).contains("test cmd", "async=false", "millis=1");
    }

    @Test
    void recordAsyncExecutionLogsAtFineLevel() {
        telemetry.recordExecution(PATH, 500_000L, true);

        assertThat(records).hasSize(1);
        var record = records.get(0);
        assertThat(record.getLevel()).isEqualTo(Level.FINE);
        assertThat(record.getMessage()).contains("async=true");
    }

    @Test
    void recordFailureLogsAtWarningLevel() {
        telemetry.recordFailure(PATH, "permission denied");

        assertThat(records).hasSize(1);
        var record = records.get(0);
        assertThat(record.getLevel()).isEqualTo(Level.WARNING);
        assertThat(record.getMessage()).contains("test cmd", "reason=permission denied");
    }

    @Test
    void recordFailureWithThrowableLogsAtWarningLevel() {
        var cause = new RuntimeException("timeout");
        telemetry.recordFailure(PATH, "execution failed", cause);

        assertThat(records).hasSize(1);
        var record = records.get(0);
        assertThat(record.getLevel()).isEqualTo(Level.WARNING);
        assertThat(record.getThrown()).isSameAs(cause);
        assertThat(record.getMessage()).contains("reason=execution failed");
    }

    @Test
    void recordSuggestionLogsAtFinerLevel() {
        telemetry.recordSuggestion(PATH, 2_000_000L, 5);

        assertThat(records).hasSize(1);
        var record = records.get(0);
        assertThat(record.getLevel()).isEqualTo(Level.FINER);
        assertThat(record.getMessage()).contains("count=5", "millis=2");
    }

    @Test
    void telemetryUsesLazyLogging() {
        telemetry.recordExecution(PATH, 0L, false);

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getMessage()).contains("test cmd");
    }
}
