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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import org.junit.jupiter.api.Test;

final class ArgumentResolverRegistryTest {

    @Test
    void emptyRegistryReturnsEmptyOptionalForAnyType() {
        var registry = ArgumentResolverRegistry.empty();

        assertThat(registry.resolver(String.class)).isEmpty();
        assertThat(registry.resolver(Integer.class)).isEmpty();
    }

    @Test
    void emptyRegistryRejectsRegistrations() {
        var registry = ArgumentResolverRegistry.empty();

        assertThatThrownBy(() -> registry.register(new StringArgumentResolver()))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Cannot register resolvers");
    }

    private static final class StringArgumentResolver implements ArgumentTypeResolver<String> {

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
