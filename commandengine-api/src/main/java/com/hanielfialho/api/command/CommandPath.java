package com.hanielfialho.api.command;

import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an immutable command path, for example {@code "guild invite"}.
 */
public record CommandPath(@NotNull List<String> parts) {

    public CommandPath {
        parts = List.copyOf(Objects.requireNonNull(parts, "parts"));
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("parts must not be empty");
        }
    }

    public CommandPath(@NotNull String... parts) {
        this(List.of(Objects.requireNonNull(parts, "parts")));
    }

    public @NotNull String root() {
        return parts.getFirst();
    }

    @Override
    public @NotNull String toString() {
        return String.join(" ", parts);
    }
}
