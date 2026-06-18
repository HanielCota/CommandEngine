package com.hanielfialho.api.rate;

import com.hanielfialho.api.command.CommandPath;
import com.hanielfialho.api.source.CommandSource;
import org.jetbrains.annotations.NotNull;

/**
 * Controls whether a sender can execute a command path at a given moment.
 */
public interface CommandRateLimiter {

    /**
     * Allows every command execution.
     */
    CommandRateLimiter NONE = (source, path) -> true;

    /**
     * Attempts to reserve one execution slot for the given source and command path.
     *
     * @param source source requesting command execution
     * @param path command path being executed
     * @return {@code true} when execution is allowed
     */
    boolean tryAcquire(@NotNull CommandSource source, @NotNull CommandPath path);
}
