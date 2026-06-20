package com.hanielfialho.runtime.internal.suggestion;

import com.hanielfialho.api.suggestion.SuggestionExecutor;
import java.time.Duration;
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
import org.jetbrains.annotations.NotNull;

public final class VirtualThreadSuggestionExecutor implements SuggestionExecutor, AutoCloseable {

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
        result.whenComplete((value, failure) -> {
            if (result.isCancelled() || failure instanceof TimeoutException) {
                futureTask.cancel(true);
            }
        });
        try {
            executor.execute(futureTask);
        } catch (RejectedExecutionException exception) {
            result.completeExceptionally(exception);
        }
        return result.orTimeout(timeout.toNanos(), TimeUnit.NANOSECONDS);
    }

    private static <T> void completeFromTask(FutureTask<T> task, CompletableFuture<T> result) {
        try {
            result.complete(task.get());
        } catch (CancellationException exception) {
            result.cancel(false);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            result.completeExceptionally(exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            result.completeExceptionally(cause == null ? exception : cause);
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
