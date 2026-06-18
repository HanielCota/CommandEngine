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
