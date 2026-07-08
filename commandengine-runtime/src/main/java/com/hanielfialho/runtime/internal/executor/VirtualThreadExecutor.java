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
import com.hanielfialho.api.message.CommandMessages;
import com.hanielfialho.api.result.CommandResult;
import com.hanielfialho.api.result.FailureReason;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.runtime.util.Preconditions;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public final class VirtualThreadExecutor implements CommandExecutor, AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(VirtualThreadExecutor.class.getName());
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
        ThreadFactory timeoutThreadFactory = Thread.ofPlatform()
                .name("commandengine-timeouts-", 0)
                .daemon(true)
                .factory();
        this.timeoutExecutor = Executors.newSingleThreadScheduledExecutor(timeoutThreadFactory);
        this.messages = Preconditions.checkNotNull(messages, "messages");
        this.timeout = timeout;
    }

    @Override
    public @NotNull CommandResult executeSync(
            @NotNull CommandSource source, @NotNull CommandPath path, @NotNull Runnable command) {
        Preconditions.checkNotNull(source, "source");
        Preconditions.checkNotNull(path, "path");
        Preconditions.checkNotNull(command, "command");
        try {
            command.run();
            return CommandResult.success();
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Command execution failed", exception);
            return CommandResult.failure(FailureReason.EXCEPTION, messages.internalError());
        }
    }

    @Override
    public @NotNull CompletableFuture<CommandResult> executeAsync(
            @NotNull CommandSource source, @NotNull CommandPath path, @NotNull Runnable command) {
        Preconditions.checkNotNull(source, "source");
        Preconditions.checkNotNull(path, "path");
        Preconditions.checkNotNull(command, "command");
        CompletableFuture<CommandResult> result = new CompletableFuture<>();
        Future<?> task;
        try {
            task = executor.submit(() -> completeResult(result, executeSync(source, path, command)));
        } catch (RejectedExecutionException _) {
            return CompletableFuture.completedFuture(
                    CommandResult.failure(FailureReason.EXCEPTION, messages.internalError()));
        }
        Future<?> timeoutTask = timeoutExecutor.schedule(
                () -> {
                    if (result.complete(CommandResult.failure(FailureReason.EXCEPTION, messages.internalError()))
                            && !task.cancel(true)) {
                        LOGGER.log(Level.FINE, "Failed to cancel timed-out command task");
                    }
                },
                timeout.toMillis(),
                TimeUnit.MILLISECONDS);
        var _ = result.whenComplete((ignoredResult, ignoredThrowable) -> {
            if (!timeoutTask.cancel(false)) {
                LOGGER.log(Level.FINE, "Failed to cancel command timeout task");
            }
        });
        return result;
    }

    @Override
    public void close() {
        shutdownAndAwait(timeoutExecutor, "timeout");
        shutdownAndAwait(executor, "task");
    }

    private static void shutdownAndAwait(ExecutorService service, String name) {
        if (service.isShutdown()) {
            return;
        }
        List<Runnable> pending = service.shutdownNow();
        if (!pending.isEmpty()) {
            LOGGER.log(Level.FINE, "Cancelled {0} pending tasks for {1} executor", new Object[] {pending.size(), name});
        }
        try {
            if (!service.awaitTermination(5, TimeUnit.SECONDS)) {
                LOGGER.log(Level.WARNING, "CommandEngine {0} executor did not terminate within 5 seconds", name);
            }
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, "Interrupted while waiting for {0} executor termination", name);
        }
    }

    @SuppressWarnings("java:S2201")
    private static <T> void completeResult(CompletableFuture<T> result, T value) {
        result.complete(value);
    }

    private static @NotNull ExecutorService createTaskExecutor() {
        try {
            return Executors.newVirtualThreadPerTaskExecutor();
        } catch (UnsupportedOperationException _) {
            return Executors.newCachedThreadPool(Thread.ofPlatform()
                    .name("commandengine-fallback-", 0)
                    .daemon(true)
                    .factory());
        }
    }
}
