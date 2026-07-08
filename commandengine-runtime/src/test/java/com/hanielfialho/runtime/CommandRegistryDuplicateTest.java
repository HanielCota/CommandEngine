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

final class CommandRegistryDuplicateTest {

    @Test
    void registerSameCommandTwice() {
        var registry = new DefaultCommandRegistry();
        var first = new SimpleAdapter("cmd");
        var second = new SimpleAdapter("cmd");

        registry.register("owner", first);

        assertThatThrownBy(() -> registry.register("owner", second))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Command already registered: cmd");
    }

    @Test
    void registerDuplicateName() {
        var registry = new DefaultCommandRegistry();
        var first = new SimpleAdapter("cmd");
        var second = new SimpleAdapter("cmd");

        registry.register("owner1", first);

        assertThatThrownBy(() -> registry.register("owner2", second))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Command already registered: cmd");
    }

    @Test
    void registerDuplicateAlias() {
        var registry = new DefaultCommandRegistry();
        var first = new SimpleAdapter("primary");
        var second = new SimpleAdapter("primary");

        registry.register("owner", first);

        assertThatThrownBy(() -> registry.register("owner", second))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Command already registered: primary");
    }

    @Test
    void conflictBetweenAliasAndMainName() {
        var registry = new DefaultCommandRegistry();
        var main = new SimpleAdapter("main");
        var withAlias = new SimpleAdapter("alias");

        registry.register("owner", main);
        registry.register("owner", withAlias);

        assertThat(registry.getAdapters()).containsExactlyInAnyOrder(main, withAlias);
    }

    @Test
    void clearErrorMessage() {
        var registry = new DefaultCommandRegistry();
        var first = new SimpleAdapter("mycommand");
        var second = new SimpleAdapter("mycommand");

        registry.register("owner", first);

        assertThatThrownBy(() -> registry.register("owner", second))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("mycommand");
    }

    @Test
    void firstRegistrationIsPreservedOnConflict() {
        var registry = new DefaultCommandRegistry();
        var first = new SimpleAdapter("cmd");
        var second = new SimpleAdapter("cmd");

        registry.register("owner", first);
        assertThatThrownBy(() -> registry.register("owner", second)).isInstanceOf(IllegalStateException.class);

        assertThat(registry.getAdapters()).containsExactly(first);
    }

    @Test
    void distinctNamesRegisterSuccessfully() {
        var registry = new DefaultCommandRegistry();
        var cmd1 = new SimpleAdapter("cmd1");
        var cmd2 = new SimpleAdapter("cmd2");

        registry.register("owner", cmd1);
        registry.register("owner", cmd2);

        assertThat(registry.getAdapters()).containsExactlyInAnyOrder(cmd1, cmd2);
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
