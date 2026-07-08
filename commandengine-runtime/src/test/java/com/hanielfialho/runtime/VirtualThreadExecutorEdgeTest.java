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

import com.hanielfialho.api.command.CommandPath;
import com.hanielfialho.api.message.CommandMessages;
import com.hanielfialho.api.result.CommandResult;
import com.hanielfialho.api.result.FailureReason;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.runtime.internal.executor.VirtualThreadExecutor;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class VirtualThreadExecutorEdgeTest {

    @Test
    void executeSyncReturnsSuccess() {
        try (var executor = new VirtualThreadExecutor()) {
            var result = executor.executeSync(source(), new CommandPath("test"), () -> {});
            assertThat(result).isInstanceOf(CommandResult.Success.class);
        }
    }

    @Test
    void executeSyncReturnsFailureOnException() {
        try (var executor = new VirtualThreadExecutor()) {
            var result = executor.executeSync(source(), new CommandPath("test"), () -> {
                throw new RuntimeException("sync fail");
            });
            assertThat(result).isInstanceOf(CommandResult.Failure.class);
            assertThat(((CommandResult.Failure) result).reason()).isEqualTo(FailureReason.EXCEPTION);
        }
    }

    @Test
    void executeSyncRejectsNullSource() {
        try (var executor = new VirtualThreadExecutor()) {
            assertThatThrownBy(() -> executor.executeSync(null, new CommandPath("test"), () -> {}))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Test
    void executeSyncRejectsNullPath() {
        try (var executor = new VirtualThreadExecutor()) {
            assertThatThrownBy(() -> executor.executeSync(source(), null, () -> {}))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Test
    void executeSyncRejectsNullCommand() {
        try (var executor = new VirtualThreadExecutor()) {
            assertThatThrownBy(() -> executor.executeSync(source(), new CommandPath("test"), null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Test
    void executeAsyncRejectsNullSource() {
        try (var executor = new VirtualThreadExecutor()) {
            assertThatThrownBy(() -> executor.executeAsync(null, new CommandPath("test"), () -> {}))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Test
    void executeAsyncRejectsNullPath() {
        try (var executor = new VirtualThreadExecutor()) {
            assertThatThrownBy(() -> executor.executeAsync(source(), null, () -> {}))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Test
    void executeAsyncRejectsNullCommand() {
        try (var executor = new VirtualThreadExecutor()) {
            assertThatThrownBy(() -> executor.executeAsync(source(), new CommandPath("test"), null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Test
    void rejectsNegativeTimeout() {
        assertThatThrownBy(() -> new VirtualThreadExecutor(CommandMessages.defaults(), Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsZeroTimeout() {
        assertThatThrownBy(() -> new VirtualThreadExecutor(CommandMessages.defaults(), Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullMessages() {
        assertThatThrownBy(() -> new VirtualThreadExecutor(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullTimeout() {
        assertThatThrownBy(() -> new VirtualThreadExecutor(CommandMessages.defaults(), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void asyncReturnsSuccessForQuickCommand() throws Exception {
        try (var executor = new VirtualThreadExecutor()) {
            var result = executor.executeAsync(source(), new CommandPath("test"), () -> {})
                    .get(1, TimeUnit.SECONDS);
            assertThat(result).isInstanceOf(CommandResult.Success.class);
        }
    }

    private static CommandSource source() {
        return new CommandSource() {
            @Override
            public boolean hasPermission(String permission) {
                return true;
            }

            @Override
            public Object getHandle() {
                return this;
            }

            @Override
            public void sendMessage(String message) {}

            @Override
            public String getName() {
                return "tester";
            }
        };
    }
}
