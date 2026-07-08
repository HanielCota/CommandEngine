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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hanielfialho.api.command.CommandAdapter;
import com.hanielfialho.api.command.CommandMetadata;
import com.hanielfialho.api.registry.BrigadierAdapter;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.runtime.internal.executor.SyncExecutor;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

final class CommandEngineReloadTest {

    @Test
    void reloadPreservesValidConfig() {
        var config = CommandEngineConfig.defaults();
        var engine = CommandEngine.builder()
                .brigadier(new TestBrigadierAdapter())
                .executor(new SyncExecutor())
                .config(config)
                .build();
        engine.close();

        var newConfig = config.withAsyncTimeout(Duration.ofSeconds(60));
        var reloaded = CommandEngine.builder()
                .brigadier(new TestBrigadierAdapter())
                .executor(new SyncExecutor())
                .config(newConfig)
                .build();

        assertThat(reloaded).isNotNull();
        assertThat(reloaded.registry().getAdapters()).isEmpty();
        reloaded.close();
    }

    @Test
    void reloadClearsOldCommands() {
        var brigadier = new TestBrigadierAdapter();
        var engine = CommandEngine.builder()
                .brigadier(brigadier)
                .executor(new SyncExecutor())
                .owner(this)
                .build();
        var adapter = new SimpleAdapter("old");
        engine.register(adapter);
        assertThat(engine.registry().getAdapters()).hasSize(1);

        engine.close();

        assertThat(engine.registry().getAdapters()).isEmpty();
        assertThat(brigadier.getDispatcher().getRoot().getChild("old")).isNull();
    }

    @Test
    void reloadPreservesPersistentCommands() {
        var config = CommandEngineConfig.defaults();
        var engine = CommandEngine.builder()
                .brigadier(new TestBrigadierAdapter())
                .config(config)
                .build();

        assertThat(engine.registry().getAdapters()).isEmpty();
        engine.close();
    }

    @Test
    void errorInReloadDoesNotCorruptRegistry() {
        var engine = CommandEngine.builder()
                .brigadier(new TestBrigadierAdapter())
                .executor(new SyncExecutor())
                .owner(this)
                .build();
        var adapter = new FailingUnregisterAdapter("fragile");
        engine.register(adapter);

        assertThatThrownBy(engine::close)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("unregister fragile");

        assertThat(engine.registry().getAdapters()).isEmpty();
    }

    @Test
    void closeAndRebuildWithDifferentOwner() {
        var brigadier = new TestBrigadierAdapter();
        var engine = CommandEngine.builder()
                .brigadier(brigadier)
                .executor(new SyncExecutor())
                .owner("alpha")
                .build();
        engine.register(new SimpleAdapter("alpha-cmd"));
        engine.close();

        var reloaded = CommandEngine.builder()
                .brigadier(new TestBrigadierAdapter())
                .executor(new SyncExecutor())
                .owner("beta")
                .build();
        reloaded.register(new SimpleAdapter("beta-cmd"));

        assertThat(reloaded.registry().getAdapters()).hasSize(1);
        reloaded.close();
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

    private static final class FailingUnregisterAdapter implements CommandAdapter {

        private final CommandMetadata metadata;

        private FailingUnregisterAdapter(String name) {
            this.metadata = new CommandMetadata(name, List.of(), "", "", List.of());
        }

        @Override
        public void register(BrigadierAdapter brigadier) {
            brigadier.register(LiteralArgumentBuilder.<CommandSource>literal(metadata.name()), metadata);
        }

        @Override
        public void unregister(BrigadierAdapter brigadier) {
            brigadier.unregister(metadata.name());
            throw new IllegalStateException("unregister " + metadata.name());
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
}
