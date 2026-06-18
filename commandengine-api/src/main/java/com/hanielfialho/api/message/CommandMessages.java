package com.hanielfialho.api.message;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * User-facing messages emitted by generated adapters and platform bridges.
 */
public record CommandMessages(
        @NotNull String internalError,
        @NotNull String invalidSender,
        @NotNull String invalidSyntax,
        @NotNull String noPermission,
        @NotNull String rateLimited) {

    public CommandMessages(
            @NotNull String internalError,
            @NotNull String invalidSender,
            @NotNull String invalidSyntax,
            @NotNull String noPermission) {
        this(internalError, invalidSender, invalidSyntax, noPermission, "You are executing this command too quickly.");
    }

    public CommandMessages {
        Objects.requireNonNull(internalError, "internalError");
        Objects.requireNonNull(invalidSender, "invalidSender");
        Objects.requireNonNull(invalidSyntax, "invalidSyntax");
        Objects.requireNonNull(noPermission, "noPermission");
        Objects.requireNonNull(rateLimited, "rateLimited");
    }

    public static @NotNull CommandMessages defaults() {
        return new CommandMessages(
                "An internal error occurred while executing this command.",
                "This command cannot be executed by this sender.",
                "Invalid command syntax.",
                "You do not have permission to execute this command.",
                "You are executing this command too quickly.");
    }
}
