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
import com.hanielfialho.runtime.internal.registry.DefaultCommandRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.List;
import org.junit.jupiter.api.Test;

final class CommandRegistrySnapshotTest {

    @Test
    void snapshotIsImmutable() {
        var registry = new DefaultCommandRegistry();
        var adapter = new SimpleAdapter("test");
        registry.register("owner", adapter);

        var snapshot = registry.getAdapters();

        assertThatThrownBy(() -> {
                    if (snapshot instanceof List<CommandAdapter> list) {
                        list.clear();
                    }
                })
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void snapshotDoesNotChangeAfterNewRegistration() {
        var registry = new DefaultCommandRegistry();
        registry.register("owner", new SimpleAdapter("first"));

        var snapshot = registry.getAdapters();
        registry.register("owner", new SimpleAdapter("second"));

        assertThat(snapshot).hasSize(1);
        assertThat(registry.getAdapters()).hasSize(2);
    }

    @Test
    void snapshotDoesNotChangeAfterUnregister() {
        var registry = new DefaultCommandRegistry();
        var first = new SimpleAdapter("first");
        var second = new SimpleAdapter("second");
        registry.register("owner", first);
        registry.register("owner", second);

        var snapshot = registry.getAdapters();
        registry.unregister(first);

        assertThat(snapshot).hasSize(2);
        assertThat(registry.getAdapters()).hasSize(1);
    }

    @Test
    void stableOrderInSnapshot() {
        var registry = new DefaultCommandRegistry();
        registry.register("owner", new SimpleAdapter("alpha"));
        registry.register("owner", new SimpleAdapter("beta"));

        var first = registry.getAdapters();
        var second = registry.getAdapters();

        assertThat(first).containsExactlyElementsOf(second);
    }

    @Test
    void multipleSnapshotsAreIndependent() {
        var registry = new DefaultCommandRegistry();
        registry.register("owner", new SimpleAdapter("only"));

        var snapshot1 = registry.getAdapters();
        var snapshot2 = registry.getAdapters();

        assertThat(snapshot1).isNotSameAs(snapshot2);
        assertThat(snapshot1).containsExactlyElementsOf(snapshot2);
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
    }
}
