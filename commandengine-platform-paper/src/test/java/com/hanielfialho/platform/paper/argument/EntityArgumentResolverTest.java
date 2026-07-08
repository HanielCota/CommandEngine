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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.entity.Entity;
import org.junit.jupiter.api.Test;

final class EntityArgumentResolverTest {

    @Test
    void entityByUUID() throws Exception {
        var uuid = UUID.randomUUID();
        var entity = entityProxy(uuid);
        var resolver = new AbstractPaperArgumentResolver<Entity>(Entity.class, "entity", name -> entity) {};
        var captured = new AtomicReference<CommandContext<Object>>();
        var dispatcher = new CommandDispatcher<Object>();

        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.<Object, String>argument("entity", argumentType(resolver))
                        .executes(context -> {
                            captured.set(context);
                            return 1;
                        })));

        dispatcher.execute("root " + uuid, new Object());

        assertThat(resolver.resolve(captured.get(), "entity")).isSameAs(entity);
    }

    @Test
    void entityNotFound() throws Exception {
        var resolver = new AbstractPaperArgumentResolver<Entity>(Entity.class, "entity", name -> null) {};
        var captured = new AtomicReference<CommandContext<Object>>();
        var dispatcher = new CommandDispatcher<Object>();

        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.<Object, String>argument("entity", argumentType(resolver))
                        .executes(context -> {
                            captured.set(context);
                            return 1;
                        })));

        dispatcher.execute("root " + UUID.randomUUID(), new Object());

        assertThatThrownBy(() -> resolver.resolve(captured.get(), "entity"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown entity");
    }

    @Test
    void wrongType() throws Exception {
        var entity = entityProxy(UUID.randomUUID());
        var resolver = new AbstractPaperArgumentResolver<Entity>(Entity.class, "entity", name -> entity) {};
        var captured = new AtomicReference<CommandContext<Object>>();
        var dispatcher = new CommandDispatcher<Object>();

        dispatcher.register(LiteralArgumentBuilder.<Object>literal("root")
                .then(RequiredArgumentBuilder.<Object, String>argument("entity", argumentType(resolver))
                        .executes(context -> {
                            captured.set(context);
                            return 1;
                        })));

        dispatcher.execute("root " + UUID.randomUUID(), new Object());

        assertThat(resolver.resolve(captured.get(), "entity")).isNotNull();
    }

    @Test
    void wrongWorld() throws Exception {
        var entity = entityProxy(UUID.randomUUID());
        var resolver = new AbstractPaperArgumentResolver<Entity>(Entity.class, "entity", name -> entity) {};
        assertThat(resolver.supportsDefault()).isTrue();
        assertThat(resolver.type()).isEqualTo(Entity.class);
    }

    @SuppressWarnings("unchecked")
    private static ArgumentType<String> argumentType(AbstractPaperArgumentResolver<?> resolver) {
        return (ArgumentType<String>) resolver.argumentType();
    }

    private static Entity entityProxy(UUID uuid) {
        return (Entity) Proxy.newProxyInstance(
                Entity.class.getClassLoader(), new Class<?>[] {Entity.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getUniqueId")) return uuid;
                    if (method.getName().equals("getName")) return uuid.toString();
                    return null;
                });
    }
}
