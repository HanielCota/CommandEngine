package com.hanielfialho.api.result;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Result of a command execution.
 */
public sealed interface CommandResult {

    static @NotNull CommandResult success() {
        return new Success(1);
    }

    static @NotNull CommandResult success(int affected) {
        return new Success(affected);
    }

    static @NotNull CommandResult failure(@NotNull FailureReason reason) {
        return new Failure(reason, null);
    }

    static @NotNull CommandResult failure(@NotNull FailureReason reason, @Nullable String message) {
        return new Failure(reason, message);
    }

    record Success(int affected) implements CommandResult {

        public Success {
            if (affected < 0) {
                throw new IllegalArgumentException("affected must be >= 0");
            }
        }
    }

    record Failure(@NotNull FailureReason reason, @Nullable String message) implements CommandResult {

        public Failure {
            Objects.requireNonNull(reason, "reason");
        }
    }
}
