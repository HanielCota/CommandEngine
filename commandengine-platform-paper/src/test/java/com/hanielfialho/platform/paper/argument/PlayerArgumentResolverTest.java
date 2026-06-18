package com.hanielfialho.platform.paper.argument;

import static org.assertj.core.api.Assertions.assertThat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

final class PlayerArgumentResolverTest {

    private static Player playerProxy(String name) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[] {Player.class},
                (proxy, method, args) -> "getName".equals(method.getName()) ? name : null);
    }

    @Test
    void resolvesPlayerByName() throws Exception {
        Player player = playerProxy("haniel");
        var resolver = new PlayerArgumentResolver(name -> "haniel".equals(name) ? player : null);
        var captured = new AtomicReference<Player>();
        var dispatcher = new CommandDispatcher<Object>();

        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.argument("player", resolver.argumentType())
                        .executes(context -> {
                            captured.set(resolver.resolve(context, "player"));
                            return 1;
                        })));

        dispatcher.execute("root haniel", new Object());

        assertThat(captured.get()).isSameAs(player);
    }

    @Test
    void resolvesFreshPlayerInstanceEachTime() throws Exception {
        Player first = playerProxy("haniel");
        Player second = playerProxy("haniel");
        var current = new AtomicReference<>(first);
        var resolver = new PlayerArgumentResolver(name -> "haniel".equals(name) ? current.get() : null);
        var captured = new AtomicReference<Player>();
        var dispatcher = new CommandDispatcher<Object>();

        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.argument("player", resolver.argumentType())
                        .executes(context -> {
                            captured.set(resolver.resolve(context, "player"));
                            return 1;
                        })));

        dispatcher.execute("root haniel", new Object());
        current.set(second);
        dispatcher.execute("root haniel", new Object());

        assertThat(captured.get()).isSameAs(second);
    }
}
