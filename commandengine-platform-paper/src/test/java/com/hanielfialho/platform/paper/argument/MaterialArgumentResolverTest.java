package com.hanielfialho.platform.paper.argument;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

final class MaterialArgumentResolverTest {

    @Test
    void resolvesMaterialByName() throws Exception {
        var resolver = new MaterialArgumentResolver(name -> Material.STONE);
        var captured = new AtomicReference<CommandContext<Object>>();
        var dispatcher = new CommandDispatcher<Object>();

        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.<Object, String>argument(
                                "material", (ArgumentType<String>) resolver.argumentType())
                        .executes(context -> {
                            captured.set(context);
                            return 1;
                        })));

        dispatcher.execute("root stone", new Object());

        assertThat(resolver.resolve(captured.get(), "material")).isEqualTo(Material.STONE);
    }

    @Test
    void throwsOnUnknownMaterial() throws Exception {
        var resolver = new MaterialArgumentResolver(name -> null);
        var captured = new AtomicReference<CommandContext<Object>>();
        var dispatcher = new CommandDispatcher<Object>();

        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.<Object, String>argument(
                                "material", (ArgumentType<String>) resolver.argumentType())
                        .executes(context -> {
                            captured.set(context);
                            return 1;
                        })));

        dispatcher.execute("root unobtainium", new Object());

        assertThatThrownBy(() -> resolver.resolve(captured.get(), "material"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown material: unobtainium");
    }
}
