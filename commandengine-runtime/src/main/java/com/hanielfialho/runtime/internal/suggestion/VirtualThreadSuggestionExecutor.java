package com.hanielfialho.runtime.internal.suggestion;

import com.hanielfialho.api.suggestion.SuggestionExecutor;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
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
        var runningTask = new AtomicReference<Future<?>>();
        result.whenComplete((value, failure) -> {
            if (result.isCancelled() || failure instanceof TimeoutException) {
                Future<?> future = runningTask.get();
                if (future != null) {
                    future.cancel(true);
                }
            }
        });
        Future<?> future = executor.submit(() -> {
            if (result.isCancelled()) {
                return;
            }
            try {
                result.complete(task.get());
            } catch (Throwable throwable) {
                result.completeExceptionally(throwable);
            }
        });
        runningTask.set(future);
        if (result.isCancelled()) {
            future.cancel(true);
        }
        return result.orTimeout(timeout.toNanos(), TimeUnit.NANOSECONDS);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
