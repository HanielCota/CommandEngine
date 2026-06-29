package com.hanielfialho.runtime.internal.executor;

import com.hanielfialho.api.executor.CommandExecutor;
import com.hanielfialho.api.message.CommandMessages;
import com.hanielfialho.api.result.CommandResult;
import com.hanielfialho.api.result.FailureReason;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.runtime.util.Preconditions;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

public final class SyncExecutor implements CommandExecutor {

    private final CommandMessages messages;

    public SyncExecutor() {
        this(CommandMessages.defaults());
    }

    public SyncExecutor(@NotNull CommandMessages messages) {
        this.messages = Preconditions.checkNotNull(messages, "messages");
    }

    @Override
    public @NotNull CommandResult executeSync(@NotNull CommandSource source, @NotNull Runnable command) {
        Preconditions.checkNotNull(source, "source");
        Preconditions.checkNotNull(command, "command");
        try {
            command.run();
            return CommandResult.success();
        } catch (Exception exception) {
            return CommandResult.failure(FailureReason.EXCEPTION, messages.internalError());
        }
    }

    @Override
    public @NotNull CompletableFuture<CommandResult> executeAsync(
            @NotNull CommandSource source, @NotNull Runnable command) {
        // Fallback: run sync if no async executor available
        return CompletableFuture.completedFuture(executeSync(source, command));
    }
}
