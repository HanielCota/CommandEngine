package com.hanielfialho.runtime.internal.executor;

import com.hanielfialho.api.command.CommandPath;
import com.hanielfialho.api.executor.CommandExecutor;
import com.hanielfialho.api.result.CommandResult;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.api.telemetry.CommandTelemetry;
import com.hanielfialho.runtime.util.Preconditions;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

public final class TelemetryCommandExecutor implements CommandExecutor, AutoCloseable {

    private final CommandExecutor delegate;
    private final CommandTelemetry telemetry;

    public TelemetryCommandExecutor(@NotNull CommandExecutor delegate, @NotNull CommandTelemetry telemetry) {
        this.delegate = Preconditions.checkNotNull(delegate, "delegate");
        this.telemetry = Preconditions.checkNotNull(telemetry, "telemetry");
    }

    @Override
    public @NotNull CommandResult executeSync(@NotNull CommandSource source, @NotNull Runnable command) {
        return delegate.executeSync(source, command);
    }

    @Override
    public @NotNull CommandResult executeSync(
            @NotNull CommandSource source, @NotNull CommandPath path, @NotNull Runnable command) {
        long started = System.nanoTime();
        CommandResult result = delegate.executeSync(source, path, command);
        record(path, result, started, false);
        return result;
    }

    @Override
    public @NotNull CompletableFuture<CommandResult> executeAsync(
            @NotNull CommandSource source, @NotNull Runnable command) {
        return delegate.executeAsync(source, command);
    }

    @Override
    public @NotNull CompletableFuture<CommandResult> executeAsync(
            @NotNull CommandSource source, @NotNull CommandPath path, @NotNull Runnable command) {
        long started = System.nanoTime();
        return delegate.executeAsync(source, path, command).whenComplete((result, throwable) -> {
            if (throwable != null) {
                telemetry.recordFailure(path, "EXCEPTION", throwable);
                return;
            }
            record(path, result, started, true);
        });
    }

    @Override
    public void close() throws Exception {
        if (delegate instanceof AutoCloseable closeable) {
            closeable.close();
        }
    }

    private void record(CommandPath path, CommandResult result, long started, boolean async) {
        long elapsed = System.nanoTime() - started;
        telemetry.recordExecution(path, elapsed, async);
        if (result instanceof CommandResult.Failure failure) {
            telemetry.recordFailure(path, failure.reason().name());
        }
    }
}
