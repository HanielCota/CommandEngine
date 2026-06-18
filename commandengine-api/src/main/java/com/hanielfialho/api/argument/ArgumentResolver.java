package com.hanielfialho.api.argument;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * SPI used to resolve an argument type from a raw string input.
 */
public interface ArgumentResolver<T> {

    @NotNull
    Class<T> type();

    @Nullable
    T resolve(@NotNull String input);
}
