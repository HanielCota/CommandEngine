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
@FunctionalInterface
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
            completeResult(result, task.get());
        } catch (CancellationException _) {
            if (!result.cancel(false)) {
                // result was already completed; nothing to do
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
}
