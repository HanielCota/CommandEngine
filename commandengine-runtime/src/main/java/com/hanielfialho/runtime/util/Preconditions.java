package com.hanielfialho.runtime.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Preconditions {

    private Preconditions() {}

    public static void checkArgument(boolean expression, @NotNull String message) {
        if (!expression) {
            throw new IllegalArgumentException(message);
        }
    }

    public static @NotNull <T> T checkNotNull(@Nullable T reference, @NotNull String message) {
        if (reference == null) {
            throw new NullPointerException(message);
        }
        return reference;
    }
}
