package com.hanielfialho.platform.paper.argument;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

final class WorldArgumentResolverTest {

    @Test
    void resolvesWorldByName() throws Exception {
        World world = worldProxy("world");
        var resolver = new WorldArgumentResolver(name -> "world".equals(name) ? world : null);
        var captured = new AtomicReference<CommandContext<Object>>();
        var dispatcher = new CommandDispatcher<Object>();

        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.<Object, String>argument("world", worldArgumentType(resolver))
                        .executes(context -> {
                            captured.set(context);
                            return 1;
                        })));

        dispatcher.execute("root world", new Object());

        assertThat(resolver.resolve(captured.get(), "world")).isSameAs(world);
    }

    @Test
    void throwsOnUnknownWorld() throws Exception {
        var resolver = new WorldArgumentResolver(name -> null);
        var captured = new AtomicReference<CommandContext<Object>>();
        var dispatcher = new CommandDispatcher<Object>();

        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.<Object, String>argument("world", worldArgumentType(resolver))
                        .executes(context -> {
                            captured.set(context);
                            return 1;
                        })));

        dispatcher.execute("root unknown", new Object());

        assertThatThrownBy(() -> resolver.resolve(captured.get(), "world"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown world: unknown");
    }

    private static World worldProxy(String name) {
        return (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[] {World.class},
                (proxy, method, args) -> "getName".equals(method.getName()) ? name : null);
    }

    @SuppressWarnings("unchecked")
    private static ArgumentType<String> worldArgumentType(WorldArgumentResolver resolver) {
        return (ArgumentType<String>) resolver.argumentType();
    }
}
