package com.hanielfialho.api.executor;

import com.hanielfialho.api.command.CommandPath;
import com.hanielfialho.api.result.CommandResult;
import com.hanielfialho.api.source.CommandSource;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

public interface CommandExecutor {

    @NotNull
    CommandResult executeSync(@NotNull CommandSource source, @NotNull Runnable command);

    default @NotNull CommandResult executeSync(
            @NotNull CommandSource source, @NotNull CommandPath path, @NotNull Runnable command) {
        return executeSync(source, command);
    }

    @NotNull
    CompletableFuture<CommandResult> executeAsync(@NotNull CommandSource source, @NotNull Runnable command);

    default @NotNull CompletableFuture<CommandResult> executeAsync(
            @NotNull CommandSource source, @NotNull CommandPath path, @NotNull Runnable command) {
        return executeAsync(source, command);
    }
}
