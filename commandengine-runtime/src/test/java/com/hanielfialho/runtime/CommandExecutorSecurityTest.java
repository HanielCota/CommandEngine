package com.hanielfialho.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.hanielfialho.api.result.CommandResult;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.runtime.internal.executor.SyncExecutor;
import com.hanielfialho.runtime.internal.executor.VirtualThreadExecutor;
import org.junit.jupiter.api.Test;

final class CommandExecutorSecurityTest {

    private static final String INTERNAL_ERROR_MESSAGE = "An internal error occurred while executing this command.";

    @Test
    void syncExecutorDoesNotExposeExceptionMessage() {
        var executor = new SyncExecutor();

        CommandResult result = executor.executeSync(new TestSource(), () -> {
            throw new IllegalStateException("secret-token");
        });

        assertThat(result).isInstanceOfSatisfying(CommandResult.Failure.class, failure -> {
            assertThat(failure.message()).isEqualTo(INTERNAL_ERROR_MESSAGE);
            assertThat(failure.message()).doesNotContain("secret-token");
        });
    }

    @Test
    void virtualThreadExecutorDoesNotExposeExceptionMessage() {
        try (var executor = new VirtualThreadExecutor()) {
            CommandResult result = executor.executeSync(new TestSource(), () -> {
                throw new IllegalStateException("secret-token");
            });

            assertThat(result).isInstanceOfSatisfying(CommandResult.Failure.class, failure -> {
                assertThat(failure.message()).isEqualTo(INTERNAL_ERROR_MESSAGE);
                assertThat(failure.message()).doesNotContain("secret-token");
            });
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
