package com.hanielfialho.test.engine;

import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.runtime.CommandEngine;
import com.hanielfialho.test.brigadier.LocalBrigadierAdapter;
import com.mojang.brigadier.CommandDispatcher;
import org.jetbrains.annotations.NotNull;

public final class TestEngine {

    private TestEngine() {}

    public static @NotNull CommandEngine create() {
        return harness().engine();
    }

    @SuppressWarnings("resource")
    public static @NotNull Harness harness() {
        var adapter = new LocalBrigadierAdapter();
        var engine = CommandEngine.builder().brigadier(adapter).build();
        return new Harness(engine, adapter.dispatcher());
    }

    public record Harness(
            @NotNull CommandEngine engine, @NotNull CommandDispatcher<CommandSource> dispatcher)
            implements AutoCloseable {

        @Override
        public void close() {
            engine.close();
        }
    }
}
