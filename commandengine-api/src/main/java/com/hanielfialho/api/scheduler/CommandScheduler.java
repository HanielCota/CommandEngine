package com.hanielfialho.api.scheduler;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * Schedules callbacks that must return to a platform-owned execution context.
 */
public interface CommandScheduler {

    CommandScheduler DIRECT =
            command -> Objects.requireNonNull(command, "command").run();

    void execute(@NotNull Runnable command);
}
