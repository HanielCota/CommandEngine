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

final class DefaultCommandRegistryTest {

    @Test
    void registersAdapterByOwner() {
        var registry = new DefaultCommandRegistry();
        var adapter = new SimpleAdapter("guild");

        registry.register("owner", adapter);

        assertThat(registry.getAdapters()).containsExactly(adapter);
        assertThat(registry.getAdapters("owner")).containsExactly(adapter);
        assertThat(registry.getAdapters("other")).isEmpty();
    }

    @Test
    void rejectsDuplicateRegistration() {
        var registry = new DefaultCommandRegistry();
        var first = new SimpleAdapter("guild");
        var second = new SimpleAdapter("guild");

        registry.register("owner", first);

        assertThatThrownBy(() -> registry.register("owner", second))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Command already registered: guild");
    }

    @Test
    void unregisterAllByOwnerRemovesOnlyOwnedAdapters() {
        var registry = new DefaultCommandRegistry();
        var owned = new SimpleAdapter("owned");
        var external = new SimpleAdapter("external");

        registry.register("owner", owned);
        registry.register("other", external);

        registry.unregisterAll("owner");

        assertThat(registry.getAdapters()).containsExactly(external);
        assertThat(registry.getAdapters("owner")).isEmpty();
        assertThat(registry.getAdapters("other")).containsExactly(external);
    }

    @Test
    void unregisterAdapterRemovesFromAllOwners() {
        var registry = new DefaultCommandRegistry();
        var adapter = new SimpleAdapter("guild");

        registry.register("owner", adapter);
        registry.unregister(adapter);

        assertThat(registry.getAdapters()).isEmpty();
        assertThat(registry.getAdapters("owner")).isEmpty();
    }

    @Test
    void unregisterDifferentAdapterKeepsRegistryUnchanged() {
        var registry = new DefaultCommandRegistry();
        var current = new SimpleAdapter("guild");
        var other = new SimpleAdapter("guild");

        registry.register("owner", current);
        registry.unregister(other);

        assertThat(registry.getAdapters()).containsExactly(current);
        assertThat(registry.getAdapters("owner")).containsExactly(current);
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
