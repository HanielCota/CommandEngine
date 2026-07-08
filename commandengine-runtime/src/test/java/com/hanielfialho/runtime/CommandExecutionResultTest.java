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
package com.hanielfialho.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hanielfialho.api.result.CommandResult;
import com.hanielfialho.api.result.FailureReason;
import org.junit.jupiter.api.Test;

final class CommandExecutionResultTest {

    @Test
    void successResult() {
        CommandResult result = CommandResult.success();
        assertThat(result).isInstanceOf(CommandResult.Success.class);
        if (result instanceof CommandResult.Success s) {
            assertThat(s.affected()).isEqualTo(1);
        }
    }

    @Test
    void successResultWithCustomAffected() {
        CommandResult result = CommandResult.success(42);
        assertThat(result).isInstanceOf(CommandResult.Success.class);
        if (result instanceof CommandResult.Success s) {
            assertThat(s.affected()).isEqualTo(42);
        }
    }

    @Test
    void successResultRejectsNegativeAffected() {
        assertThatThrownBy(() -> CommandResult.success(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void argumentErrorResult() {
        CommandResult result = CommandResult.failure(FailureReason.INVALID_ARGUMENT);
        assertThat(result).isInstanceOf(CommandResult.Failure.class);
        if (result instanceof CommandResult.Failure f) {
            assertThat(f.reason()).isEqualTo(FailureReason.INVALID_ARGUMENT);
            assertThat(f.message()).isNull();
        }
    }

    @Test
    void permissionErrorResult() {
        CommandResult result = CommandResult.failure(FailureReason.NO_PERMISSION, "No permission");
        assertThat(result).isInstanceOf(CommandResult.Failure.class);
        if (result instanceof CommandResult.Failure f) {
            assertThat(f.reason()).isEqualTo(FailureReason.NO_PERMISSION);
            assertThat(f.message()).isEqualTo("No permission");
        }
    }

    @Test
    void internalErrorResult() {
        CommandResult result = CommandResult.failure(FailureReason.EXCEPTION, "Internal error");
        assertThat(result).isInstanceOf(CommandResult.Failure.class);
        if (result instanceof CommandResult.Failure f) {
            assertThat(f.reason()).isEqualTo(FailureReason.EXCEPTION);
            assertThat(f.message()).isEqualTo("Internal error");
        }
    }

    @Test
    void cancelledByMiddleware() {
        CommandResult result = CommandResult.failure(FailureReason.CIRCUIT_OPEN);
        assertThat(result).isInstanceOf(CommandResult.Failure.class);
        if (result instanceof CommandResult.Failure f) {
            assertThat(f.reason()).isEqualTo(FailureReason.CIRCUIT_OPEN);
            assertThat(f.message()).isNull();
        }
    }

    @Test
    void failureRejectsNullReason() {
        assertThatThrownBy(() -> CommandResult.failure(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void successRecordHasAffectedField() {
        var success = new CommandResult.Success(5);
        assertThat(success.affected()).isEqualTo(5);
    }

    @Test
    void failureRecordHasReasonAndMessageFields() {
        var failure = new CommandResult.Failure(FailureReason.INVALID_ARGUMENT, "bad input");
        assertThat(failure.reason()).isEqualTo(FailureReason.INVALID_ARGUMENT);
        assertThat(failure.message()).isEqualTo("bad input");
    }

    @Test
    void failureRecordAllowsNullMessage() {
        var failure = new CommandResult.Failure(FailureReason.RATE_LIMITED, null);
        assertThat(failure.reason()).isEqualTo(FailureReason.RATE_LIMITED);
        assertThat(failure.message()).isNull();
    }

    @Test
    void sealedInterfacePatternMatchSuccess() {
        CommandResult result = CommandResult.success();
        String description =
                switch (result) {
                    case CommandResult.Success s -> "success:" + s.affected();
                    case CommandResult.Failure f -> "failure:" + f.reason();
                };
        assertThat(description).isEqualTo("success:1");
    }

    @Test
    void sealedInterfacePatternMatchFailure() {
        CommandResult result = CommandResult.failure(FailureReason.EXCEPTION);
        String description =
                switch (result) {
                    case CommandResult.Success s -> "success:" + s.affected();
                    case CommandResult.Failure f -> "failure:" + f.reason();
                };
        assertThat(description).isEqualTo("failure:EXCEPTION");
    }
}
