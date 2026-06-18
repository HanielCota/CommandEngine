package com.hanielfialho.processor.model;

import java.util.Objects;
import org.jetbrains.annotations.Nullable;

public record ParameterModel(
        String name,
        String typeName,
        Kind kind,
        boolean optional,
        @Nullable String defaultValue,
        boolean greedy,
        @Nullable Double min,
        @Nullable Double max,
        char shorthand,
        @Nullable String suggestionMethodName) {

    public ParameterModel {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(typeName, "typeName");
        Objects.requireNonNull(kind, "kind");
    }

    public String getName() {
        return name;
    }

    public String getTypeName() {
        return typeName;
    }

    public Kind getKind() {
        return kind;
    }

    public boolean isOptional() {
        return optional;
    }

    public @Nullable String getDefaultValue() {
        return defaultValue;
    }

    public boolean isGreedy() {
        return greedy;
    }

    public @Nullable Double getMin() {
        return min;
    }

    public @Nullable Double getMax() {
        return max;
    }

    public char getShorthand() {
        return shorthand;
    }

    public @Nullable String getSuggestionMethodName() {
        return suggestionMethodName;
    }

    public enum Kind {
        SENDER,
        ARGUMENT,
        FLAG
    }
}
