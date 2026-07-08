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
import com.hanielfialho.api.registry.BrigadierAdapter;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.runtime.internal.registry.DefaultCommandRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.List;
import org.junit.jupiter.api.Test;

final class CommandUnregistrationTest {

    @Test
    void unregisterByName() {
        var registry = new DefaultCommandRegistry();
        var adapter = new SimpleAdapter("test");

        registry.register("owner", adapter);
        registry.unregister(adapter);

        assertThat(registry.getAdapters()).isEmpty();
        assertThat(registry.getAdapters("owner")).isEmpty();
    }

    @Test
    void unregisterByInstance() {
        var registry = new DefaultCommandRegistry();
        var adapter1 = new SimpleAdapter("cmd1");
        var adapter2 = new SimpleAdapter("cmd2");

        registry.register("owner", adapter1);
        registry.register("owner", adapter2);
        registry.unregister(adapter1);

        assertThat(registry.getAdapters()).containsExactly(adapter2);
        assertThat(registry.getAdapters("owner")).containsExactly(adapter2);
    }

    @Test
    void unregisterNonExistent() {
        var registry = new DefaultCommandRegistry();
        var adapter = new SimpleAdapter("phantom");

        registry.unregister(adapter);

        assertThat(registry.getAdapters()).isEmpty();
    }

    @Test
    void unregisterDifferentInstanceWithSameNameDoesNotRemove() {
        var registry = new DefaultCommandRegistry();
        var registered = new SimpleAdapter("cmd");
        var impostor = new SimpleAdapter("cmd");

        registry.register("owner", registered);
        registry.unregister(impostor);

        assertThat(registry.getAdapters()).containsExactly(registered);
        assertThat(registry.getAdapters("owner")).containsExactly(registered);
    }

    @Test
    void unregisterRemovesAliasesFromOwnerIndex() {
        var registry = new DefaultCommandRegistry();
        var adapter = new SimpleAdapter("test");

        registry.register("owner", adapter);
        registry.unregister(adapter);

        assertThat(registry.getAdapters("owner")).isEmpty();
    }

    @Test
    void unregisterDoesNotAffectOtherOwners() {
        var registry = new DefaultCommandRegistry();
        var owned = new SimpleAdapter("owned");
        var external = new SimpleAdapter("external");

        registry.register("owner", owned);
        registry.register("other", external);
        registry.unregister(owned);

        assertThat(registry.getAdapters()).containsExactly(external);
        assertThat(registry.getAdapters("other")).containsExactly(external);
    }

    @Test
    void unregisterAllRemovesAllForOwner() {
        var registry = new DefaultCommandRegistry();
        var adapter = new SimpleAdapter("test");

        registry.register("owner", adapter);
        registry.unregisterAll("owner");

        assertThat(registry.getAdapters()).isEmpty();
        assertThat(registry.getAdapters("owner")).isEmpty();
    }

    @Test
    void unregisterAllForUnknownOwnerDoesNothing() {
        var registry = new DefaultCommandRegistry();
        registry.unregisterAll("unknown");

        assertThat(registry.getAdapters()).isEmpty();
    }

    @Test
    void unregisterAllPreservesAdaptersOfOtherOwners() {
        var registry = new DefaultCommandRegistry();
        var owned = new SimpleAdapter("owned");
        var external = new SimpleAdapter("external");

        registry.register("owner", owned);
        registry.register("other", external);
        registry.unregisterAll("owner");

        assertThat(registry.getAdapters()).containsExactly(external);
        assertThat(registry.getAdapters("other")).containsExactly(external);
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
