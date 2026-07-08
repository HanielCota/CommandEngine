/*
 * Copyright (c) 2026 Haniel Fialho
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.hanielfialho.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.hanielfialho.api.command.CommandAdapter;
import com.hanielfialho.api.command.CommandMetadata;
import com.hanielfialho.api.executor.CommandExecutor;
import com.hanielfialho.api.registry.BrigadierAdapter;
import com.hanielfialho.api.result.CommandResult;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.runtime.internal.executor.SyncExecutor;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

final class CommandEngineLifecycleTest {

    @Test
    void startEngine() {
        var engine =
                CommandEngine.builder().brigadier(new TestBrigadierAdapter()).build();

        assertThat(engine).isNotNull();
        assertThat(engine.registry()).isNotNull();
        engine.close();
    }

    @Test
    void registerAfterStart() {
        var brigadier = new TestBrigadierAdapter();
        try (var engine = CommandEngine.builder()
                .brigadier(brigadier)
                .executor(new SyncExecutor())
                .owner(this)
                .build()) {
            var adapter = new SimpleAdapter("hello");

            engine.register(adapter);

            assertThat(engine.registry().getAdapters()).containsExactly(adapter);
        }
    }

    @Test
    void shutdown() {
        var engine =
                CommandEngine.builder().brigadier(new TestBrigadierAdapter()).build();

        engine.close();

        assertThat(engine.registry().getAdapters()).isEmpty();
    }

    @Test
    void executeAfterShutdownFails() {
        var brigadier = new TestBrigadierAdapter();
        var engine = CommandEngine.builder()
                .brigadier(brigadier)
                .executor(new SyncExecutor())
                .owner(this)
                .build();
        var adapter = new SimpleAdapter("temp");
        engine.register(adapter);

        engine.close();

        assertThat(engine.registry().getAdapters()).isEmpty();
        assertThat(brigadier.getDispatcher().getRoot().getChild("temp")).isNull();
    }

    @Test
    void registerAfterCloseStillWorks() {
        var brigadier = new TestBrigadierAdapter();
        var engine = CommandEngine.builder()
                .brigadier(brigadier)
                .executor(new SyncExecutor())
                .owner(this)
                .build();
        engine.close();

        var adapter = new SimpleAdapter("after");
        engine.register(adapter);

        assertThat(engine.registry().getAdapters()).containsExactly(adapter);
    }

    @Test
    void closeIsIdempotent() {
        var engine =
                CommandEngine.builder().brigadier(new TestBrigadierAdapter()).build();

        engine.close();
        engine.close();

        assertThat(engine.registry().getAdapters()).isEmpty();
    }

    @Test
    void autoCloseableTryWithResources() {
        var brigadier = new TestBrigadierAdapter();
        try (var engine = CommandEngine.builder()
                .brigadier(brigadier)
                .executor(new SyncExecutor())
                .owner(this)
                .build()) {
            engine.register(new SimpleAdapter("auto"));
            assertThat(engine.registry().getAdapters()).hasSize(1);
        }
    }

    @Test
    void customExecutorNotClosedOnShutdown() {
        var executor = new TrackedExecutor();
        try (var engine = CommandEngine.builder()
                .brigadier(new TestBrigadierAdapter())
                .executor(executor)
                .build()) {
            engine.register(new SimpleAdapter("tracked"));
        }

        assertThat(executor.closed).isFalse();
    }

    private static final class SimpleAdapter implements CommandAdapter {

        private final CommandMetadata metadata;

        private SimpleAdapter(String name) {
            this.metadata = new CommandMetadata(name, List.of(), "", "", List.of());
        }

        @Override
        public void register(BrigadierAdapter brigadier) {
            brigadier.register(LiteralArgumentBuilder.<CommandSource>literal(metadata.name()), metadata);
        }

        @Override
        public void unregister(BrigadierAdapter brigadier) {
            brigadier.unregister(metadata.name());
        }

        @Override
        public CommandMetadata metadata() {
            return metadata;
        }
    }

    private static final class TestBrigadierAdapter implements BrigadierAdapter {

        private final CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher<>();

        @Override
        public CommandDispatcher<CommandSource> getDispatcher() {
            return dispatcher;
        }

        @Override
        public void unregister(String name) {
            TestBrigadierRootMutator.remove(dispatcher, name);
        }
    }

    private static final class TrackedExecutor implements CommandExecutor {

        private boolean closed;

        @Override
        public CommandResult executeSync(
                CommandSource source, com.hanielfialho.api.command.CommandPath path, Runnable command) {
            command.run();
            return CommandResult.success();
        }

        @Override
        public CompletableFuture<CommandResult> executeAsync(
                CommandSource source, com.hanielfialho.api.command.CommandPath path, Runnable command) {
            return CompletableFuture.completedFuture(executeSync(source, path, command));
        }
    }
}
