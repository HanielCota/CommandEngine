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
package com.hanielfialho.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hanielfialho.runtime.internal.suggestion.VirtualThreadSuggestionExecutor;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Test;

final class VirtualThreadSuggestionExecutorTest {

    @Test
    void returnsCompletedFutureWithTaskResult() throws Exception {
        try (var executor = new VirtualThreadSuggestionExecutor()) {
            var future = executor.submit(() -> "result");
            assertThat(future.get(1, TimeUnit.SECONDS)).isEqualTo("result");
        }
    }

    @Test
    void propagatesTaskException() {
        try (var executor = new VirtualThreadSuggestionExecutor()) {
            var future = executor.submit(() -> {
                throw new IllegalStateException("task error");
            });
            assertThatThrownBy(() -> future.get(1, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(IllegalStateException.class)
                    .hasStackTraceContaining("task error");
        }
    }

    @Test
    void timesOutWhenTaskTakesTooLong() {
        try (var executor = new VirtualThreadSuggestionExecutor(Duration.ofMillis(50))) {
            var future = executor.submit(() -> {
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(10));
                return "too late";
            });
            assertThatThrownBy(() -> future.get(2, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(TimeoutException.class);
        }
    }

    @Test
    void rejectsNullTask() {
        try (var executor = new VirtualThreadSuggestionExecutor()) {
            assertThatThrownBy(() -> executor.submit(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("task");
        }
    }

    @Test
    void closeIsIdempotent() {
        var executor = new VirtualThreadSuggestionExecutor();
        executor.close();
        executor.close();
    }

    @Test
    void rejectsTaskAfterClose() throws Exception {
        var executor = new VirtualThreadSuggestionExecutor();
        executor.close();
        var future = executor.submit(() -> "value");
        assertThatThrownBy(() -> future.get(1, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class);
    }
}
