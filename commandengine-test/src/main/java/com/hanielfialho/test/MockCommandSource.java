package com.hanielfialho.test;

import org.jetbrains.annotations.NotNull;

/**
 * @deprecated Use {@link com.hanielfialho.test.source.MockCommandSource} instead.
 */
@Deprecated(forRemoval = true)
public final class MockCommandSource extends com.hanielfialho.test.source.MockCommandSource {

    public MockCommandSource(@NotNull String name) {
        super(name);
    }
}
