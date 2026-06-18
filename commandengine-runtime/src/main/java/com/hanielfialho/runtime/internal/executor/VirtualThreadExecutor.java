package com.hanielfialho.runtime.internal.executor;

import com.hanielfialho.api.executor.CommandExecutor;
import com.hanielfialho.api.message.CommandMessages;
import com.hanielfialho.api.result.CommandResult;
import com.hanielfialho.api.result.FailureReason;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.runtime.util.Preconditions;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

public final class VirtualThreadExecutor implements CommandExecutor, AutoCloseable {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final ExecutorService executor;
    private final CommandMessages messages;
    private final Duration timeout;

    public VirtualThreadExecutor() {
        this(CommandMessages.defaults());
    }

    public VirtualThreadExecutor(@NotNull CommandMessages messages) {
        this(messages, DEFAULT_TIMEOUT);
    }

    public VirtualThreadExecutor(@NotNull CommandMessages messages, @NotNull Duration timeout) {
        Preconditions.checkNotNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.messages = Preconditions.checkNotNull(messages, "messages");
        this.timeout = timeout;
    }

    @Override
    public @NotNull CommandResult executeSync(@NotNull CommandSource source, @NotNull Runnable command) {
        Preconditions.checkNotNull(source, "source");
        Preconditions.checkNotNull(command, "command");
        try {
            command.run();
            return CommandResult.success();
        } catch (RuntimeException exception) {
            return CommandResult.failure(FailureReason.EXCEPTION, messages.internalError());
        }
    }

    @Override
    public @NotNull CompletableFuture<CommandResult> executeAsync(
            @NotNull CommandSource source, @NotNull Runnable command) {
        Preconditions.checkNotNull(source, "source");
        Preconditions.checkNotNull(command, "command");
        return CompletableFuture.supplyAsync(() -> executeSync(source, command), executor)
                .completeOnTimeout(
                        CommandResult.failure(FailureReason.EXCEPTION, messages.internalError()),
                        timeout.toMillis(),
                        TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        executor.close();
    }
}
