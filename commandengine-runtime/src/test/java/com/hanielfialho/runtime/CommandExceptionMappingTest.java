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

import com.hanielfialho.api.command.CommandPath;
import com.hanielfialho.api.result.CommandResult;
import com.hanielfialho.api.result.FailureReason;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.runtime.internal.executor.SyncExecutor;
import com.hanielfialho.runtime.internal.executor.VirtualThreadExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

final class CommandExceptionMappingTest {

    private final SyncExecutor syncExecutor = new SyncExecutor();
    private final VirtualThreadExecutor asyncExecutor = new VirtualThreadExecutor();
    private final CommandPath path = new CommandPath("test");
    private final CommandSource source = source();

    @Test
    void illegalArgumentException() {
        CommandResult result = syncExecutor.executeSync(source, path, () -> {
            throw new IllegalArgumentException("bad arg");
        });
        assertThat(result).isInstanceOf(CommandResult.Failure.class);
        assertThat(((CommandResult.Failure) result).reason()).isEqualTo(FailureReason.EXCEPTION);
    }

    @Test
    void securityException() {
        CommandResult result = syncExecutor.executeSync(source, path, () -> {
            throw new SecurityException("access denied");
        });
        assertThat(result).isInstanceOf(CommandResult.Failure.class);
        assertThat(((CommandResult.Failure) result).reason()).isEqualTo(FailureReason.EXCEPTION);
    }

    @Test
    void customException() {
        CommandResult result = syncExecutor.executeSync(source, path, () -> {
            throw new CustomCommandException("something broke");
        });
        assertThat(result).isInstanceOf(CommandResult.Failure.class);
        assertThat(((CommandResult.Failure) result).reason()).isEqualTo(FailureReason.EXCEPTION);
    }

    @Test
    void unknownException() {
        CommandResult result = syncExecutor.executeSync(source, path, () -> {
            throw new RuntimeException("unknown failure");
        });
        assertThat(result).isInstanceOf(CommandResult.Failure.class);
        assertThat(((CommandResult.Failure) result).reason()).isEqualTo(FailureReason.EXCEPTION);
    }

    @Test
    void chainedCause() {
        CommandResult result = syncExecutor.executeSync(source, path, () -> {
            throw new RuntimeException("outer", new IllegalArgumentException("inner cause"));
        });
        assertThat(result).isInstanceOf(CommandResult.Failure.class);
        assertThat(((CommandResult.Failure) result).reason()).isEqualTo(FailureReason.EXCEPTION);
    }

    @Test
    void asyncIllegalArgumentException() throws Exception {
        CommandResult result = asyncExecutor
                .executeAsync(source, path, () -> {
                    throw new IllegalArgumentException("bad arg");
                })
                .get(1, TimeUnit.SECONDS);
        assertThat(result).isInstanceOf(CommandResult.Failure.class);
        assertThat(((CommandResult.Failure) result).reason()).isEqualTo(FailureReason.EXCEPTION);
    }

    @Test
    void asyncChainedCause() throws Exception {
        CommandResult result = asyncExecutor
                .executeAsync(source, path, () -> {
                    throw new RuntimeException("outer", new SecurityException("inner"));
                })
                .get(1, TimeUnit.SECONDS);
        assertThat(result).isInstanceOf(CommandResult.Failure.class);
        assertThat(((CommandResult.Failure) result).reason()).isEqualTo(FailureReason.EXCEPTION);
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

    private static final class CustomCommandException extends RuntimeException {
        CustomCommandException(String message) {
            super(message);
        }
    }
}
