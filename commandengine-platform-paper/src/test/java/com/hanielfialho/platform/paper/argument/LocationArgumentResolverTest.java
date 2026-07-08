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

import com.hanielfialho.api.source.CommandSource;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.junit.jupiter.api.Test;

final class LocationArgumentResolverTest {

    @Test
    void resolvesLocationFromEntitySource() throws Exception {
        World world = worldProxy("world");
        Entity entity = entityProxy(world);
        CommandSource source = sourceProxy(entity);
        var resolver = new LocationArgumentResolver();
        var captured = new AtomicReference<CommandContext<CommandSource>>();
        var dispatcher = new CommandDispatcher<CommandSource>();

        dispatcher.register(LiteralArgumentBuilder.<CommandSource>literal("root")
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("loc", locationArgumentType(resolver))
                        .executes(context -> {
                            captured.set(context);
                            return 1;
                        })));

        dispatcher.execute("root 10,20,30", source);

        Location location = resolver.resolve(captured.get(), "loc");
        assertThat(location.getX()).isEqualTo(10.0);
        assertThat(location.getY()).isEqualTo(20.0);
        assertThat(location.getZ()).isEqualTo(30.0);
        assertThat(location.getWorld()).isSameAs(world);
    }

    @Test
    void rejectsInvalidLocationFormat() throws Exception {
        var resolver = new LocationArgumentResolver();
        var captured = new AtomicReference<CommandContext<CommandSource>>();
        var dispatcher = new CommandDispatcher<CommandSource>();

        dispatcher.register(LiteralArgumentBuilder.<CommandSource>literal("root")
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("loc", locationArgumentType(resolver))
                        .executes(context -> {
                            captured.set(context);
                            return 1;
                        })));

        dispatcher.execute("root 10,20", sourceProxy(entityProxy(worldProxy("world"))));

        assertThatThrownBy(() -> resolver.resolve(captured.get(), "loc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Location must be x,y,z or world,x,y,z");
    }

    @Test
    void rejectsNonNumericCoordinate() throws Exception {
        var resolver = new LocationArgumentResolver();
        var captured = new AtomicReference<CommandContext<CommandSource>>();
        var dispatcher = new CommandDispatcher<CommandSource>();

        dispatcher.register(LiteralArgumentBuilder.<CommandSource>literal("root")
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("loc", locationArgumentType(resolver))
                        .executes(context -> {
                            captured.set(context);
                            return 1;
                        })));

        dispatcher.execute("root 10,abc,30", sourceProxy(entityProxy(worldProxy("world"))));

        assertThatThrownBy(() -> resolver.resolve(captured.get(), "loc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Location coordinate must be a number");
    }

    @SuppressWarnings("unchecked")
    private static ArgumentType<String> locationArgumentType(LocationArgumentResolver resolver) {
        return (ArgumentType<String>) resolver.argumentType();
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.util.function.Function<String, Object> handler) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> handler.apply(method.getName()));
    }

    private static World worldProxy(String name) {
        return proxy(World.class, method -> "getName".equals(method) ? name : null);
    }

    private static Entity entityProxy(World world) {
        return proxy(Entity.class, method -> "getWorld".equals(method) ? world : null);
    }

    private static CommandSource sourceProxy(Entity entity) {
        return proxy(CommandSource.class, method -> switch (method) {
            case "getHandle" -> entity;
            case "hasPermission" -> true;
            case "getName" -> "test";
            default -> null;
        });
    }
}
