package com.hanielfialho.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hanielfialho.api.result.CommandResult;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.runtime.internal.executor.SyncExecutor;
import com.hanielfialho.runtime.internal.executor.VirtualThreadExecutor;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
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

    @Test
    void syncExecutorHandlesErrorWithoutExposingMessage() {
        var executor = new SyncExecutor();

        assertThatThrownBy(() -> executor.executeSync(new TestSource(), () -> {
                    throw new AssertionError("secret-token");
                }))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("secret-token");
    }

    @Test
    void virtualThreadExecutorHandlesErrorWithoutExposingMessage() {
        try (var executor = new VirtualThreadExecutor()) {
            assertThatThrownBy(() -> executor.executeSync(new TestSource(), () -> {
                        throw new AssertionError("secret-token");
                    }))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("secret-token");
        }
    }

    @Test
    void virtualThreadExecutorCancelsTimedOutTask() throws Exception {
        try (var executor = new VirtualThreadExecutor(
                com.hanielfialho.api.message.CommandMessages.defaults(), Duration.ofMillis(50))) {
            var started = new CountDownLatch(1);
            var interrupted = new CountDownLatch(1);

            CommandResult result = executor.executeAsync(new TestSource(), () -> {
                        started.countDown();
                        while (!Thread.currentThread().isInterrupted()) {
                            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
                        }
                        if (Thread.currentThread().isInterrupted()) {
                            interrupted.countDown();
                        }
                    })
                    .get(1, TimeUnit.SECONDS);

            assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(result).isInstanceOf(CommandResult.Failure.class);
            assertThat(interrupted.await(1, TimeUnit.SECONDS)).isTrue();
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
