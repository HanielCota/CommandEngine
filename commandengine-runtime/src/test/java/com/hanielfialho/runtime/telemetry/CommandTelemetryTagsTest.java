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
package com.hanielfialho.runtime.telemetry;

import static org.assertj.core.api.Assertions.assertThat;

import com.hanielfialho.api.command.CommandPath;
import com.hanielfialho.api.executor.CommandExecutor;
import com.hanielfialho.api.result.CommandResult;
import com.hanielfialho.api.result.FailureReason;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.api.telemetry.CommandTelemetry;
import com.hanielfialho.runtime.internal.executor.TelemetryCommandExecutor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

final class CommandTelemetryTagsTest {

    private static final CommandPath PATH = new CommandPath("guild", "create");
    private static final CommandSource SOURCE = new TestSource();

    @Test
    void commandTag() {
        var telemetry = new RecordingTelemetry();
        var delegate = new DelegateExecutor(CommandResult.success());
        var executor = new TelemetryCommandExecutor(delegate, telemetry);

        executor.executeSync(SOURCE, PATH, () -> {});

        assertThat(telemetry.executions).hasSize(1);
        assertThat(telemetry.executions.get(0).path).isEqualTo(PATH);
    }

    @Test
    void senderTag() {
        var telemetry = new RecordingTelemetry();
        var delegate = new DelegateExecutor(CommandResult.failure(FailureReason.NO_PERMISSION));
        var executor = new TelemetryCommandExecutor(delegate, telemetry);

        executor.executeSync(SOURCE, PATH, () -> {});

        assertThat(telemetry.failures).hasSize(1);
        assertThat(telemetry.failures.get(0).reason).contains("NO_PERMISSION");
    }

    @Test
    void resultTag() {
        var telemetry = new RecordingTelemetry();

        var successDelegate = new DelegateExecutor(CommandResult.success());
        var successExecutor = new TelemetryCommandExecutor(successDelegate, telemetry);
        successExecutor.executeSync(SOURCE, PATH, () -> {});

        assertThat(telemetry.executions).hasSize(1);
        assertThat(telemetry.failures).isEmpty();

        var failureDelegate = new DelegateExecutor(CommandResult.failure(FailureReason.INVALID_ARGUMENT));
        var failureExecutor = new TelemetryCommandExecutor(failureDelegate, telemetry);
        failureExecutor.executeSync(SOURCE, PATH, () -> {});

        assertThat(telemetry.executions).hasSize(2);
        assertThat(telemetry.failures).hasSize(1);
        assertThat(telemetry.failures.get(0).reason).contains("INVALID_ARGUMENT");
    }

    @Test
    void exceptionTag() {
        var telemetry = new RecordingTelemetry();
        var delegate = new DelegateExecutor(CommandResult.failure(FailureReason.EXCEPTION));
        var executor = new TelemetryCommandExecutor(delegate, telemetry);

        executor.executeSync(SOURCE, PATH, () -> {});

        assertThat(telemetry.failures).hasSize(1);
        assertThat(telemetry.failures.get(0).reason).contains("EXCEPTION");
    }

    private record ExecutionRecord(CommandPath path, long nanos, boolean async) {}

    private record FailureRecord(CommandPath path, String reason) {}

    private static final class RecordingTelemetry implements CommandTelemetry {

        private final List<ExecutionRecord> executions = new ArrayList<>();
        private final List<FailureRecord> failures = new ArrayList<>();

        @Override
        public void recordExecution(CommandPath path, long nanos, boolean async) {
            executions.add(new ExecutionRecord(path, nanos, async));
        }

        @Override
        public void recordFailure(CommandPath path, String reason) {
            failures.add(new FailureRecord(path, reason));
        }

        @Override
        public void recordFailure(CommandPath path, String reason, Throwable throwable) {
            failures.add(new FailureRecord(path, reason));
        }

        @Override
        public void recordSuggestion(CommandPath path, long nanos, int suggestionCount) {
            // no-op: not tested here
        }
    }

    private static final class DelegateExecutor implements CommandExecutor {

        private final CommandResult result;

        DelegateExecutor(CommandResult result) {
            this.result = result;
        }

        @Override
        public CommandResult executeSync(CommandSource source, CommandPath path, Runnable command) {
            return result;
        }

        @Override
        public CompletableFuture<CommandResult> executeAsync(CommandSource source, CommandPath path, Runnable command) {
            return CompletableFuture.completedFuture(result);
        }
    }

    private static final class TestSource implements CommandSource {

        @Override
        public boolean hasPermission(String permission) {
            return true;
        }

        @Override
        public Object getHandle() {
            return this;
        }

        @Override
        public void sendMessage(String message) {
            // no-op: test source
        }

        @Override
        public String getName() {
            return "test";
        }
    }
}
