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
package com.hanielfialho.api.argument;

import static org.assertj.core.api.Assertions.assertThat;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class ArgumentResolverPriorityTest {

    @Test
    void specificResolverWinsOverGeneric() {
        var registry = new MapArgumentResolverRegistry();
        registry.register(new IntegerResolver());
        registry.register(new StringResolver());

        assertThat(registry.resolver(Integer.class)).isPresent();
        assertThat(registry.resolver(String.class)).isPresent();
    }

    @Test
    void duplicateResolverOverwrites() {
        var registry = new MapArgumentResolverRegistry();
        registry.register(new IntegerResolver());
        registry.register(new IntegerResolver());

        var resolved = registry.resolver(Integer.class);
        assertThat(resolved).isPresent();
        assertThat(resolved.get().type()).isEqualTo(Integer.class);
    }

    @Test
    void registrationOrderIsRespected() {
        var registry = new MapArgumentResolverRegistry();
        registry.register(new StringResolver());
        registry.register(new IntegerResolver());

        var intResolver = registry.resolver(Integer.class);
        assertThat(intResolver).isPresent();
        assertThat(intResolver.get().type()).isEqualTo(Integer.class);
    }

    private static final class MapArgumentResolverRegistry implements ArgumentResolverRegistry {

        private final Map<Class<?>, ArgumentTypeResolver<?>> resolvers = new HashMap<>();

        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<ArgumentTypeResolver<T>> resolver(Class<T> type) {
            return Optional.ofNullable((ArgumentTypeResolver<T>) resolvers.get(type));
        }

        @Override
        public <T> ArgumentResolverRegistry register(ArgumentTypeResolver<T> resolver) {
            resolvers.put(resolver.type(), resolver);
            return this;
        }
    }

    private static final class IntegerResolver implements ArgumentTypeResolver<Integer> {

        @Override
        public Class<Integer> type() {
            return Integer.class;
        }

        @Override
        public ArgumentType<?> argumentType() {
            return StringArgumentType.word();
        }

        @Override
        public Integer resolve(CommandContext<?> context, String name) {
            return 1;
        }
    }

    private static final class StringResolver implements ArgumentTypeResolver<String> {

        @Override
        public Class<String> type() {
            return String.class;
        }

        @Override
        public ArgumentType<?> argumentType() {
            return StringArgumentType.string();
        }

        @Override
        public String resolve(CommandContext<?> context, String name) {
            return StringArgumentType.getString(context, name);
        }
    }
}
