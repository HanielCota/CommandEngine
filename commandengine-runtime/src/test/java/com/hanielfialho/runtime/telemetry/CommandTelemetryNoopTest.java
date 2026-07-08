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

import static org.assertj.core.api.Assertions.assertThatCode;

import com.hanielfialho.api.command.CommandPath;
import com.hanielfialho.api.telemetry.CommandTelemetry;
import org.junit.jupiter.api.Test;

final class CommandTelemetryNoopTest {

    private static final CommandPath PATH = new CommandPath("test", "cmd");
    private static final CommandTelemetry NOOP = CommandTelemetry.NOOP;

    @Test
    void noopDoesNotFail() {
        assertThatCode(() -> {
                    NOOP.recordExecution(PATH, 1000L, false);
                    NOOP.recordFailure(PATH, "reason");
                    NOOP.recordFailure(PATH, "reason", new RuntimeException("fail"));
                    NOOP.recordSuggestion(PATH, 500L, 3);
                })
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsSuccess() {
        assertThatCode(() -> NOOP.recordExecution(PATH, 1_000_000L, false)).doesNotThrowAnyException();
    }

    @Test
    void acceptsError() {
        assertThatCode(() -> NOOP.recordFailure(PATH, "no permission")).doesNotThrowAnyException();
    }

    @Test
    void acceptsZeroDuration() {
        assertThatCode(() -> NOOP.recordExecution(PATH, 0L, false)).doesNotThrowAnyException();
    }
}
