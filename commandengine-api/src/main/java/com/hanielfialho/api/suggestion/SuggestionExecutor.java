package com.hanielfialho.api.suggestion;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
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
            try {
                return CompletableFuture.completedFuture(task.get());
            } catch (Throwable throwable) {
                var result = new CompletableFuture<T>();
                result.completeExceptionally(throwable);
                return result;
            }
        }
    };

    <T> @NotNull CompletableFuture<T> submit(@NotNull Supplier<T> task);
}
