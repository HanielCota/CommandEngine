package com.hanielfialho.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.hanielfialho.api.result.CommandResult;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.runtime.internal.executor.VirtualThreadExecutor;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Test;

final class VirtualThreadExecutorTest {

    private static final String INTERNAL_ERROR_MESSAGE = "An internal error occurred while executing this command.";

    @Test
    void returnsFailureWhenExecutorIsShutdown() throws Exception {
        var executor = new VirtualThreadExecutor();
        executor.close();

        CommandResult result = executor.executeAsync(new TestSource(), () -> {}).get(1, TimeUnit.SECONDS);

        assertThat(result).isInstanceOf(CommandResult.Failure.class);
        assertThat(((CommandResult.Failure) result).message()).isEqualTo(INTERNAL_ERROR_MESSAGE);
    }

    @Test
    void timeoutCancelsRunningTask() throws Exception {
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

    @Test
    void closeIsIdempotent() {
        var executor = new VirtualThreadExecutor();
        executor.close();
        executor.close();
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
