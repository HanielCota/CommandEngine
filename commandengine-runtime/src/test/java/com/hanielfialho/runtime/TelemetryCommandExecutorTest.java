package com.hanielfialho.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.hanielfialho.api.command.CommandPath;
import com.hanielfialho.api.result.CommandResult;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.api.telemetry.CommandTelemetry;
import com.hanielfialho.runtime.internal.executor.SyncExecutor;
import com.hanielfialho.runtime.internal.executor.TelemetryCommandExecutor;
import org.junit.jupiter.api.Test;

final class TelemetryCommandExecutorTest {

    @Test
    void telemetryFailureDoesNotFailSuccessfulCommand() {
        var executor = new TelemetryCommandExecutor(new SyncExecutor(), new FailingTelemetry());

        CommandResult result = executor.executeSync(new TestSource(), new CommandPath("root"), () -> {});

        assertThat(result).isInstanceOf(CommandResult.Success.class);
    }

    private static final class FailingTelemetry implements CommandTelemetry {

        @Override
        public void recordExecution(CommandPath path, long nanos, boolean async) {
            throw new IllegalStateException("telemetry unavailable");
        }

        @Override
        public void recordFailure(CommandPath path, String reason) {
            throw new IllegalStateException("telemetry unavailable");
        }

        @Override
        public void recordSuggestion(CommandPath path, long nanos, int suggestionCount) {
            throw new IllegalStateException("telemetry unavailable");
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
        public void sendMessage(String message) {}

        @Override
        public String getName() {
            return "test";
        }
    }
}
