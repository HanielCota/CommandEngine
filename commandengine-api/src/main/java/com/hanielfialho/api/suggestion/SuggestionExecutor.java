package com.hanielfialho.api.suggestion;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

/**
 * Executes tab-completion work for suggestion providers that explicitly opt in to async execution.
 */
public interface SuggestionExecutor {

    SuggestionExecutor DIRECT = new SuggestionExecutor() {
        @Override
        public <T> @NotNull CompletableFuture<T> submit(@NotNull Supplier<T> task) {
            Objects.requireNonNull(task, "task");
            var result = new CompletableFuture<T>();
            var directTask = new FutureTask<T>(task::get) {
                @Override
                protected void done() {
                    completeFromTask(this, result);
                }
            };
            directTask.run();
            return result;
        }
    };

    <T> @NotNull CompletableFuture<T> submit(@NotNull Supplier<T> task);

    private static <T> void completeFromTask(FutureTask<T> task, CompletableFuture<T> result) {
        try {
            result.complete(task.get());
        } catch (CancellationException _) {
            if (!result.cancel(false)) {
                // result was already completed; nothing to do
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            result.completeExceptionally(exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            result.completeExceptionally(cause == null ? exception : cause);
        }
    }
}
