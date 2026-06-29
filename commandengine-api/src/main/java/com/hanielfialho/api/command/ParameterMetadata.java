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
        if (!optional && defaultValue != null) {
            throw new IllegalArgumentException("defaultValue requires optional=true");
        }
        if (optional && type.isPrimitive() && (defaultValue == null || defaultValue.isBlank())) {
            throw new IllegalArgumentException("primitive optional parameter requires a non-blank defaultValue");
        }
    }

    public enum ParameterKind {
        SENDER,
        ARGUMENT,
        FLAG
    }
}
