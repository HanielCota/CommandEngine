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

import com.hanielfialho.api.argument.ArgumentTypeResolver;
import com.hanielfialho.runtime.internal.argument.DefaultArgumentResolverRegistry;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import org.junit.jupiter.api.Test;

final class DefaultArgumentResolverRegistryEdgeTest {

    private final DefaultArgumentResolverRegistry registry = new DefaultArgumentResolverRegistry();

    @Test
    void resolvesString() {
        assertThat(registry.resolver(String.class)).isPresent();
    }

    @Test
    void resolvesInteger() {
        assertThat(registry.resolver(Integer.class)).isPresent();
    }

    @Test
    void resolvesInt() {
        assertThat(registry.resolver(int.class)).isPresent();
    }

    @Test
    void resolvesBoolean() {
        assertThat(registry.resolver(Boolean.class)).isPresent();
    }

    @Test
    void resolvesBool() {
        assertThat(registry.resolver(boolean.class)).isPresent();
    }

    @Test
    void resolvesLong() {
        assertThat(registry.resolver(Long.class)).isPresent();
    }

    @Test
    void resolvesLongPrimitive() {
        assertThat(registry.resolver(long.class)).isPresent();
    }

    @Test
    void resolvesFloat() {
        assertThat(registry.resolver(Float.class)).isPresent();
    }

    @Test
    void resolvesFloatPrimitive() {
        assertThat(registry.resolver(float.class)).isPresent();
    }

    @Test
    void resolvesDouble() {
        assertThat(registry.resolver(Double.class)).isPresent();
    }

    @Test
    void resolvesDoublePrimitive() {
        assertThat(registry.resolver(double.class)).isPresent();
    }

    @Test
    void doesNotResolveUnknownType() {
        assertThat(registry.resolver(Object.class)).isNotPresent();
    }

    @Test
    void customResolverCanBeRegistered() {
        var custom = new ArgumentTypeResolver<String>() {
            @Override
            public Class<String> type() {
                return String.class;
            }

            @Override
            public ArgumentType<?> argumentType() {
                return com.mojang.brigadier.arguments.StringArgumentType.string();
            }

            @Override
            public String resolve(CommandContext<?> ctx, String name) {
                return "custom";
            }
        };
        registry.register(custom);
        assertThat(registry.resolver(String.class)).hasValue(custom);
    }

    @Test
    void rejectsNullResolver() {
        assertThatThrownBy(() -> registry.register(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullType() {
        assertThatThrownBy(() -> registry.resolver(null)).isInstanceOf(NullPointerException.class);
    }
}
