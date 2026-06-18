package com.hanielfialho.runtime.internal.executor;

import com.hanielfialho.api.executor.CommandExecutor;
import com.hanielfialho.api.message.CommandMessages;
import com.hanielfialho.api.result.CommandResult;
import com.hanielfialho.api.result.FailureReason;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.runtime.util.Preconditions;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import org.jetbrains.annotations.NotNull;

public final class VirtualThreadExecutor implements CommandExecutor, AutoCloseable {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final ExecutorService executor;
    private final ScheduledExecutorService timeoutExecutor;
    private final CommandMessages messages;
    private final Duration timeout;

    public VirtualThreadExecutor() {
        this(CommandMessages.defaults());
    }

    public VirtualThreadExecutor(@NotNull CommandMessages messages) {
        this(messages, DEFAULT_TIMEOUT);
    }

    public VirtualThreadExecutor(@NotNull CommandMessages messages, @NotNull Duration timeout) {
        Preconditions.checkNotNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        this.executor = createTaskExecutor();
        this.timeoutExecutor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "commandengine-timeouts");
            thread.setDaemon(true);
            return thread;
        });
        this.messages = Preconditions.checkNotNull(messages, "messages");
        this.timeout = timeout;
    }

    @Override
    public @NotNull CommandResult executeSync(@NotNull CommandSource source, @NotNull Runnable command) {
        Preconditions.checkNotNull(source, "source");
        Preconditions.checkNotNull(command, "command");
        try {
            command.run();
            return CommandResult.success();
        } catch (Throwable exception) {
            return CommandResult.failure(FailureReason.EXCEPTION, messages.internalError());
        }
    }

    @Override
    public @NotNull CompletableFuture<CommandResult> executeAsync(
            @NotNull CommandSource source, @NotNull Runnable command) {
        Preconditions.checkNotNull(source, "source");
        Preconditions.checkNotNull(command, "command");
        CompletableFuture<CommandResult> result = new CompletableFuture<>();
        Future<?> task;
        try {
            task = executor.submit(() -> result.complete(executeSync(source, command)));
        } catch (RejectedExecutionException exception) {
            return CompletableFuture.completedFuture(
                    CommandResult.failure(FailureReason.EXCEPTION, messages.internalError()));
        }
        Future<?> timeoutTask = timeoutExecutor.schedule(
                () -> {
                    if (!result.isDone()) {
                        result.complete(CommandResult.failure(FailureReason.EXCEPTION, messages.internalError()));
                        task.cancel(true);
                    }
                },
                timeout.toMillis(),
                java.util.concurrent.TimeUnit.MILLISECONDS);
        result.whenComplete((ignoredResult, ignoredThrowable) -> timeoutTask.cancel(false));
        return result;
    }

    @Override
    public void close() {
        if (!timeoutExecutor.isShutdown()) {
            timeoutExecutor.shutdownNow();
        }
        if (!executor.isShutdown()) {
            executor.close();
        }
    }

    private static @NotNull ExecutorService createTaskExecutor() {
        try {
            return Executors.newVirtualThreadPerTaskExecutor();
        } catch (UnsupportedOperationException exception) {
            return Executors.newCachedThreadPool();
        }
    }
}
