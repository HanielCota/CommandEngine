package com.hanielfialho.api.command;

import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable command metadata.
 */
public record CommandMetadata(
        @NotNull String name,
        @NotNull List<String> aliases,
        @NotNull String description,
        @NotNull String permission,
        @NotNull List<SubcommandMetadata> subcommands) {

    public CommandMetadata {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        aliases = List.copyOf(Objects.requireNonNull(aliases, "aliases"));
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(permission, "permission");
        subcommands = List.copyOf(Objects.requireNonNull(subcommands, "subcommands"));
    }
}
