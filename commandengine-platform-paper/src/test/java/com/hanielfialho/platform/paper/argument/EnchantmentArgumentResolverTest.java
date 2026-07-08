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

import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.enchantments.Enchantment;
import org.junit.jupiter.api.Test;

final class EnchantmentArgumentResolverTest {

    @Test
    void validEnchantment() {
        var captured = new AtomicReference<String>();
        var resolver = new AbstractPaperArgumentResolver<Enchantment>(Enchantment.class, "enchantment", name -> {
            captured.set(name);
            return null;
        }) {};

        assertThatThrownBy(() -> resolver.resolveDefault(mockContext(), "sharpness"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(captured.get()).isEqualTo("sharpness");
    }

    @Test
    void namespacedKey() {
        var captured = new AtomicReference<String>();
        var resolver = new AbstractPaperArgumentResolver<Enchantment>(Enchantment.class, "enchantment", name -> {
            captured.set(name);
            return null;
        }) {};

        assertThatThrownBy(() -> resolver.resolveDefault(mockContext(), "minecraft:sharpness"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(captured.get()).isEqualTo("minecraft:sharpness");
    }

    @Test
    void invalidName() {
        var resolver =
                new AbstractPaperArgumentResolver<Enchantment>(Enchantment.class, "enchantment", name -> null) {};
        assertThatThrownBy(() -> resolver.resolveDefault(mockContext(), "unknown_enchant"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown enchantment: unknown_enchant");
    }

    @Test
    void suggestionByPrefix() {
        var resolver =
                new AbstractPaperArgumentResolver<Enchantment>(Enchantment.class, "enchantment", name -> null) {};
        assertThat(resolver.supportsDefault()).isTrue();
        assertThat(resolver.type()).isEqualTo(Enchantment.class);
    }

    private static com.mojang.brigadier.context.CommandContext<?> mockContext() {
        return new com.mojang.brigadier.context.CommandContextBuilder(
                        new com.mojang.brigadier.CommandDispatcher<>(),
                        new Object(),
                        new com.mojang.brigadier.tree.RootCommandNode<>(),
                        0)
                .build("test");
    }
}
