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
package com.hanielfialho.runtime.internal.suggestion;

import com.hanielfialho.api.suggestion.SuggestionExecutor;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public final class VirtualThreadSuggestionExecutor implements SuggestionExecutor, AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(VirtualThreadSuggestionExecutor.class.getName());

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Duration timeout;

    public VirtualThreadSuggestionExecutor() {
        this(Duration.ofSeconds(30));
    }

    public VirtualThreadSuggestionExecutor(@NotNull Duration timeout) {
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        this.timeout = timeout;
    }

    @Override
    public <T> @NotNull CompletableFuture<T> submit(@NotNull Supplier<T> task) {
        Objects.requireNonNull(task, "task");
        var result = new CompletableFuture<T>();
        var futureTask = new FutureTask<T>(task::get) {
            @Override
            protected void done() {
                completeFromTask(this, result);
            }
        };
        var _ = result.whenComplete((value, failure) -> {
            if ((result.isCancelled() || failure instanceof TimeoutException) && !futureTask.cancel(true)) {
                LOGGER.log(Level.FINE, "Failed to cancel timed-out suggestion task");
            }
        });
        try {
            executor.execute(futureTask);
        } catch (RejectedExecutionException exception) {
            completeExceptionally(result, exception);
        }
        return result.orTimeout(timeout.toNanos(), TimeUnit.NANOSECONDS);
    }

    private static <T> void completeFromTask(FutureTask<T> task, CompletableFuture<T> result) {
        try {
            completeResult(result, task.get());
        } catch (CancellationException _) {
            if (!result.cancel(false)) {
                LOGGER.log(Level.FINE, "Failed to cancel suggestion result");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            completeExceptionally(result, exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            completeExceptionally(result, cause == null ? exception : cause);
        }
    }

    @SuppressWarnings("java:S2201")
    private static <T> void completeResult(CompletableFuture<T> result, T value) {
        result.complete(value);
    }

    @SuppressWarnings("java:S2201")
    private static <T> void completeExceptionally(CompletableFuture<T> result, Throwable throwable) {
        result.completeExceptionally(throwable);
    }

    @Override
    public void close() {
        List<Runnable> pending = executor.shutdownNow();
        if (!pending.isEmpty()) {
            LOGGER.log(Level.FINE, "Cancelled {0} pending suggestion tasks", pending.size());
        }
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                LOGGER.log(Level.WARNING, "CommandEngine suggestion executor did not terminate within 5 seconds");
            }
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, "Interrupted while waiting for suggestion executor termination");
        }
    }
}
