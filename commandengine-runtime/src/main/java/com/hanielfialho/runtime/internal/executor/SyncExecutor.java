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
package com.hanielfialho.runtime.internal.executor;

import com.hanielfialho.api.command.CommandPath;
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
    public @NotNull CommandResult executeSync(
            @NotNull CommandSource source, @NotNull CommandPath path, @NotNull Runnable command) {
        Preconditions.checkNotNull(source, "source");
        Preconditions.checkNotNull(path, "path");
        Preconditions.checkNotNull(command, "command");
        try {
            command.run();
            return CommandResult.success();
        } catch (Exception _) {
            return CommandResult.failure(FailureReason.EXCEPTION, messages.internalError());
        }
    }

    @Override
    public @NotNull CompletableFuture<CommandResult> executeAsync(
            @NotNull CommandSource source, @NotNull CommandPath path, @NotNull Runnable command) {
        // Fallback: run sync if no async executor available
        return CompletableFuture.completedFuture(executeSync(source, path, command));
    }
}
