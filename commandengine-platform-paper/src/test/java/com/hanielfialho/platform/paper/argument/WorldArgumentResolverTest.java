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
