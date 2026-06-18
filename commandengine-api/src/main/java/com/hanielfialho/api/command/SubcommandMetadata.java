package com.hanielfialho.api.command;

import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record SubcommandMetadata(
        @NotNull String path,
        @NotNull String permission,
        @NotNull String description,
        boolean async,
        @NotNull List<ParameterMetadata> parameters) {

    public SubcommandMetadata {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(permission, "permission");
        Objects.requireNonNull(description, "description");
        parameters = List.copyOf(Objects.requireNonNull(parameters, "parameters"));
    }
}
