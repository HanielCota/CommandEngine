package com.hanielfialho.test;

import com.hanielfialho.runtime.CommandEngine;
import com.hanielfialho.test.engine.TestEngine.Harness;
import org.jetbrains.annotations.NotNull;

/**
 * @deprecated Use {@link com.hanielfialho.test.engine.TestEngine} instead.
 */
@Deprecated(forRemoval = true)
public final class TestEngine {

    private TestEngine() {}

    public static @NotNull CommandEngine create() {
        return com.hanielfialho.test.engine.TestEngine.create();
    }

    public static @NotNull Harness harness() {
        return com.hanielfialho.test.engine.TestEngine.harness();
    }
}
