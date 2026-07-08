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

import com.hanielfialho.api.message.CommandMessages;
import com.hanielfialho.api.result.CommandResult;
import com.hanielfialho.api.result.FailureReason;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.runtime.internal.executor.VirtualThreadExecutor;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Test;

final class CommandTimeoutTest {

    private static final String INTERNAL_ERROR = "An internal error occurred while executing this command.";

    @Test
    void fastCommandDoesNotExpire() throws Exception {
        try (var executor = new VirtualThreadExecutor(CommandMessages.defaults(), Duration.ofMillis(500))) {

            CommandResult result = executor.executeAsync(source(), () -> {}).get(1, TimeUnit.SECONDS);

            assertThat(result).isInstanceOf(CommandResult.Success.class);
        }
    }

    @Test
    void slowCommandExpires() throws Exception {
        try (var executor = new VirtualThreadExecutor(CommandMessages.defaults(), Duration.ofMillis(20))) {

            CommandResult result = executor.executeAsync(source(), () -> {
                        while (!Thread.currentThread().isInterrupted()) {
                            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
                        }
                    })
                    .get(5, TimeUnit.SECONDS);

            assertThat(result).isInstanceOf(CommandResult.Failure.class);
            assertThat(((CommandResult.Failure) result).reason()).isEqualTo(FailureReason.EXCEPTION);
            assertThat(((CommandResult.Failure) result).message()).isEqualTo(INTERNAL_ERROR);
        }
    }

    @Test
    void customTimeoutPerCommand() throws Exception {
        try (var executor = new VirtualThreadExecutor(CommandMessages.defaults(), Duration.ofMillis(200))) {

            CommandResult result = executor.executeAsync(source(), () -> {
                        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50));
                    })
                    .get(1, TimeUnit.SECONDS);

            assertThat(result).isInstanceOf(CommandResult.Success.class);
        }
    }

    @Test
    void globalTimeout() throws Exception {
        try (var executor = new VirtualThreadExecutor(CommandMessages.defaults(), Duration.ofMillis(10))) {

            CommandResult result = executor.executeAsync(source(), () -> {
                        while (!Thread.currentThread().isInterrupted()) {
                            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(5));
                        }
                    })
                    .get(5, TimeUnit.SECONDS);

            assertThat(result).isInstanceOf(CommandResult.Failure.class);
            assertThat(((CommandResult.Failure) result).message()).isEqualTo(INTERNAL_ERROR);
        }
    }

    @Test
    void timeoutDoesNotKillMainThread() throws Exception {
        var mainThread = Thread.currentThread();
        var mainAlive = new AtomicBoolean(true);

        try (var executor = new VirtualThreadExecutor(CommandMessages.defaults(), Duration.ofMillis(20))) {

            executor.executeAsync(source(), () -> {
                        while (!Thread.currentThread().isInterrupted()) {
                            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
                        }
                    })
                    .get(5, TimeUnit.SECONDS);

            assertThat(mainThread.isAlive()).isTrue();
            assertThat(mainAlive.get()).isTrue();
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
            public void sendMessage(String message) {
                // no-op: test stub
            }

            @Override
            public String getName() {
                return "test";
            }
        };
    }
}
