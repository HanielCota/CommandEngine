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
package com.hanielfialho.api.result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

final class CommandResultTest {

    @Test
    void successFactoryCreatesSuccessWithOneAffected() {
        var result = CommandResult.success();
        assertThat(result).isInstanceOf(CommandResult.Success.class);
        assertThat(((CommandResult.Success) result).affected()).isEqualTo(1);
    }

    @Test
    void successWithAffectedCreatesSuccessWithGivenValue() {
        var result = CommandResult.success(5);
        assertThat(result).isInstanceOf(CommandResult.Success.class);
        assertThat(((CommandResult.Success) result).affected()).isEqualTo(5);
    }

    @Test
    void successWithZeroAffectedIsValid() {
        var result = new CommandResult.Success(0);
        assertThat(result.affected()).isEqualTo(0);
    }

    @Test
    void successThrowsOnNegativeAffected() {
        assertThatThrownBy(() -> new CommandResult.Success(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("affected must be >= 0");
    }

    @Test
    void failureWithoutMessageCreatesFailureWithNullMessage() {
        var result = CommandResult.failure(FailureReason.NO_PERMISSION);
        assertThat(result).isInstanceOf(CommandResult.Failure.class);
        var failure = (CommandResult.Failure) result;
        assertThat(failure.reason()).isEqualTo(FailureReason.NO_PERMISSION);
        assertThat(failure.message()).isNull();
    }

    @Test
    void failureWithMessageCreatesFailureWithGivenMessage() {
        var result = CommandResult.failure(FailureReason.INVALID_ARGUMENT, "bad value");
        assertThat(result).isInstanceOf(CommandResult.Failure.class);
        var failure = (CommandResult.Failure) result;
        assertThat(failure.reason()).isEqualTo(FailureReason.INVALID_ARGUMENT);
        assertThat(failure.message()).isEqualTo("bad value");
    }

    @Test
    void failureThrowsOnNullReason() {
        assertThatThrownBy(() -> new CommandResult.Failure(null, "msg"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("reason");
    }

    @Test
    void failureAcceptsNullMessage() {
        var result = new CommandResult.Failure(FailureReason.EXCEPTION, null);
        assertThat(result.message()).isNull();
    }
}
