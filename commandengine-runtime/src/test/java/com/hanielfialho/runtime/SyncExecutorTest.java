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
import com.hanielfialho.runtime.internal.executor.SyncExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class SyncExecutorTest {

    private final SyncExecutor executor = new SyncExecutor();

    @Test
    void executesSyncSuccessfully() {
        var result = executor.executeSync(source(), new CommandPath("test"), () -> {});
        assertThat(result).isInstanceOf(CommandResult.Success.class);
    }

    @Test
    void returnsExceptionFailureOnCommandError() {
        var result = executor.executeSync(source(), new CommandPath("test"), () -> {
            throw new RuntimeException("fail");
        });
        assertThat(result).isInstanceOf(CommandResult.Failure.class);
        assertThat(((CommandResult.Failure) result).reason()).isEqualTo(FailureReason.EXCEPTION);
    }

    @Test
    void asyncDelegatesToSync() throws Exception {
        var result = executor.executeAsync(source(), new CommandPath("test"), () -> {})
                .get(1, TimeUnit.SECONDS);
        assertThat(result).isInstanceOf(CommandResult.Success.class);
    }

    @Test
    void asyncReturnsFailureWhenCommandFails() throws Exception {
        var result = executor.executeAsync(source(), new CommandPath("test"), () -> {
                    throw new RuntimeException("async fail");
                })
                .get(1, TimeUnit.SECONDS);
        assertThat(result).isInstanceOf(CommandResult.Failure.class);
        assertThat(((CommandResult.Failure) result).reason()).isEqualTo(FailureReason.EXCEPTION);
    }

    @Test
    void rejectsNullSource() {
        assertThatThrownBy(() -> executor.executeSync(null, new CommandPath("test"), () -> {}))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullPath() {
        assertThatThrownBy(() -> executor.executeSync(source(), null, () -> {}))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullCommand() {
        assertThatThrownBy(() -> executor.executeSync(source(), new CommandPath("test"), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullSourceOnAsync() {
        assertThatThrownBy(() -> executor.executeAsync(null, new CommandPath("test"), () -> {}))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void usesCustomMessagesOnError() {
        var customMsg = "custom error";
        var msg = new CommandMessages(customMsg, "b", "c", "d", "e");
        var sync = new SyncExecutor(msg);
        var result = sync.executeSync(source(), new CommandPath("test"), () -> {
            throw new RuntimeException("fail");
        });
        assertThat(result).isInstanceOf(CommandResult.Failure.class);
        assertThat(((CommandResult.Failure) result).message()).isEqualTo(customMsg);
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
                return "tester";
            }
        };
    }
}
