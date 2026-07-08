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
package com.hanielfialho.runtime.internal.executor;

import com.hanielfialho.api.command.CommandPath;
import com.hanielfialho.api.executor.CommandExecutor;
import com.hanielfialho.api.result.CommandResult;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.api.telemetry.CommandTelemetry;
import com.hanielfialho.runtime.util.Preconditions;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public final class TelemetryCommandExecutor implements CommandExecutor, AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(TelemetryCommandExecutor.class.getName());

    private final CommandExecutor delegate;
    private final CommandTelemetry telemetry;

    public TelemetryCommandExecutor(@NotNull CommandExecutor delegate, @NotNull CommandTelemetry telemetry) {
        this.delegate = Preconditions.checkNotNull(delegate, "delegate");
        this.telemetry = Preconditions.checkNotNull(telemetry, "telemetry");
    }

    @Override
    public @NotNull CommandResult executeSync(
            @NotNull CommandSource source, @NotNull CommandPath path, @NotNull Runnable command) {
        long started = System.nanoTime();
        CommandResult result = delegate.executeSync(source, path, command);
        recordResult(path, result, started, false);
        return result;
    }

    @Override
    public @NotNull CompletableFuture<CommandResult> executeAsync(
            @NotNull CommandSource source, @NotNull CommandPath path, @NotNull Runnable command) {
        long started = System.nanoTime();
        return delegate.executeAsync(source, path, command).whenComplete((result, throwable) -> {
            if (throwable != null) {
                long elapsed = System.nanoTime() - started;
                recordTelemetry(() -> telemetry.recordExecution(path, elapsed, true));
                recordTelemetry(() -> telemetry.recordFailure(path, "EXCEPTION", throwable));
                return;
            }
            recordResult(path, result, started, true);
        });
    }

    @Override
    public void close() throws Exception {
        if (delegate instanceof AutoCloseable closeable) {
            closeable.close();
        }
    }

    private void recordResult(CommandPath path, CommandResult result, long started, boolean async) {
        long elapsed = System.nanoTime() - started;
        recordTelemetry(() -> telemetry.recordExecution(path, elapsed, async));
        if (result instanceof CommandResult.Failure failure) {
            recordTelemetry(() -> telemetry.recordFailure(path, failure.reason().name()));
        }
    }

    private void recordTelemetry(Runnable telemetryCall) {
        try {
            telemetryCall.run();
        } catch (RuntimeException exception) {
            LOGGER.log(Level.FINE, exception, () -> "Command telemetry callback failed");
        }
    }
}
