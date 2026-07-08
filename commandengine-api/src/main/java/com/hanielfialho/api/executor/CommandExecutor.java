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
package com.hanielfialho.api.executor;

import com.hanielfialho.api.command.CommandPath;
import com.hanielfialho.api.result.CommandResult;
import com.hanielfialho.api.source.CommandSource;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

public interface CommandExecutor {

    /**
     * Executes a command synchronously, preserving the command path for telemetry and rate limiting.
     */
    @NotNull
    CommandResult executeSync(@NotNull CommandSource source, @NotNull CommandPath path, @NotNull Runnable command);

    /**
     * Executes a command synchronously without a specific path.
     * Delegates to {@link #executeSync(CommandSource, CommandPath, Runnable)}.
     */
    default @NotNull CommandResult executeSync(@NotNull CommandSource source, @NotNull Runnable command) {
        return executeSync(source, CommandPath.empty(), command);
    }

    /**
     * Executes a command asynchronously, preserving the command path for telemetry and rate limiting.
     */
    @NotNull
    CompletableFuture<CommandResult> executeAsync(
            @NotNull CommandSource source, @NotNull CommandPath path, @NotNull Runnable command);

    /**
     * Executes a command asynchronously without a specific path.
     * Delegates to {@link #executeAsync(CommandSource, CommandPath, Runnable)}.
     */
    default @NotNull CompletableFuture<CommandResult> executeAsync(
            @NotNull CommandSource source, @NotNull Runnable command) {
        return executeAsync(source, CommandPath.empty(), command);
    }
}
