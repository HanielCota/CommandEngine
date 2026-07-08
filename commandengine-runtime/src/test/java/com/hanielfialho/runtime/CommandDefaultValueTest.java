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
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import org.junit.jupiter.api.Test;

final class CommandDefaultValueTest {

    @Test
    void defaultString() {
        var resolver = new ArgumentTypeResolver<String>() {
            @Override
            public Class<String> type() {
                return String.class;
            }

            @Override
            public ArgumentType<?> argumentType() {
                return StringArgumentType.word();
            }

            @Override
            public String resolve(CommandContext<?> context, String name) {
                return context.getArgument(name, String.class);
            }

            @Override
            public String resolveDefault(CommandContext<?> context, String input) {
                return "fallback";
            }

            @Override
            public boolean supportsDefault() {
                return true;
            }
        };
        assertThat(resolver.supportsDefault()).isTrue();
        assertThat(resolver.resolveDefault(null, "")).isEqualTo("fallback");
    }

    @Test
    void defaultInt() {
        var resolver = new ArgumentTypeResolver<Integer>() {
            @Override
            public Class<Integer> type() {
                return Integer.class;
            }

            @Override
            public ArgumentType<?> argumentType() {
                return IntegerArgumentType.integer();
            }

            @Override
            public Integer resolve(CommandContext<?> context, String name) {
                return context.getArgument(name, Integer.class);
            }

            @Override
            public Integer resolveDefault(CommandContext<?> context, String input) {
                return 42;
            }

            @Override
            public boolean supportsDefault() {
                return true;
            }
        };
        assertThat(resolver.supportsDefault()).isTrue();
        assertThat(resolver.resolveDefault(null, "")).isEqualTo(42);
    }

    @Test
    void defaultBoolean() {
        var resolver = new ArgumentTypeResolver<Boolean>() {
            @Override
            public Class<Boolean> type() {
                return Boolean.class;
            }

            @Override
            public ArgumentType<?> argumentType() {
                return BoolArgumentType.bool();
            }

            @Override
            public Boolean resolve(CommandContext<?> context, String name) {
                return context.getArgument(name, Boolean.class);
            }

            @Override
            public Boolean resolveDefault(CommandContext<?> context, String input) {
                return true;
            }

            @Override
            public boolean supportsDefault() {
                return true;
            }
        };
        assertThat(resolver.supportsDefault()).isTrue();
        assertThat(resolver.resolveDefault(null, "")).isTrue();
    }

    @Test
    void defaultInvalidForType() {
        var resolver = new ArgumentTypeResolver<Integer>() {
            @Override
            public Class<Integer> type() {
                return Integer.class;
            }

            @Override
            public ArgumentType<?> argumentType() {
                return IntegerArgumentType.integer();
            }

            @Override
            public Integer resolve(CommandContext<?> context, String name) {
                return context.getArgument(name, Integer.class);
            }

            @Override
            public Integer resolveDefault(CommandContext<?> context, String input) {
                throw new UnsupportedOperationException("Cannot determine default for " + type().getName());
            }

            @Override
            public boolean supportsDefault() {
                return true;
            }
        };
        assertThat(resolver.supportsDefault()).isTrue();
        assertThatThrownBy(() -> resolver.resolveDefault(null, ""))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Integer");
    }

    @Test
    void dynamicDefaultProvider() {
        var resolver = new ArgumentTypeResolver<Integer>() {
            private int counter;

            @Override
            public Class<Integer> type() {
                return Integer.class;
            }

            @Override
            public ArgumentType<?> argumentType() {
                return IntegerArgumentType.integer();
            }

            @Override
            public Integer resolve(CommandContext<?> context, String name) {
                return context.getArgument(name, Integer.class);
            }

            @Override
            public Integer resolveDefault(CommandContext<?> context, String input) {
                return ++counter;
            }

            @Override
            public boolean supportsDefault() {
                return true;
            }
        };
        assertThat(resolver.resolveDefault(null, "")).isEqualTo(1);
        assertThat(resolver.resolveDefault(null, "")).isEqualTo(2);
        assertThat(resolver.resolveDefault(null, "")).isEqualTo(3);
    }
}
