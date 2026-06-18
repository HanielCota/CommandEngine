package com.hanielfialho.api.command;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record ParameterMetadata(
        @NotNull String name,
        @NotNull Class<?> type,
        @NotNull ParameterKind kind,
        @Nullable String defaultValue,
        boolean optional) {

    public ParameterMetadata {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(kind, "kind");
    }

    public enum ParameterKind {
        SENDER,
        ARGUMENT,
        FLAG
    }
}
