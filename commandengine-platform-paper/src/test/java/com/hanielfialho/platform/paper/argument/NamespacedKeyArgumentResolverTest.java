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

import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

final class NamespacedKeyArgumentResolverTest {

    @Test
    void minecraftStone() {
        var resolver = new AbstractPaperArgumentResolver<NamespacedKey>(
                NamespacedKey.class, "namespacedKey", NamespacedKey::fromString) {};

        var result = resolver.resolveDefault(mockContext(), "minecraft:stone");

        assertThat(result.getNamespace()).isEqualTo("minecraft");
        assertThat(result.getKey()).isEqualTo("stone");
    }

    @Test
    void keyWithoutNamespace() {
        var resolver = new AbstractPaperArgumentResolver<NamespacedKey>(NamespacedKey.class, "namespacedKey", s -> {
            if (s.contains(":")) {
                return NamespacedKey.fromString(s);
            }
            return NamespacedKey.minecraft(s);
        }) {};

        var result = resolver.resolveDefault(mockContext(), "stone");

        assertThat(result.getNamespace()).isEqualTo("minecraft");
        assertThat(result.getKey()).isEqualTo("stone");
    }

    @Test
    void invalidNamespace() {
        var resolver = new AbstractPaperArgumentResolver<NamespacedKey>(NamespacedKey.class, "namespacedKey", s -> {
            try {
                return NamespacedKey.fromString(s);
            } catch (Exception e) {
                return null;
            }
        }) {};

        assertThatThrownBy(() -> resolver.resolveDefault(mockContext(), "InvalidNamespace:stone"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown namespacedKey");
    }

    @Test
    void invalidCharacters() {
        var resolver = new AbstractPaperArgumentResolver<NamespacedKey>(NamespacedKey.class, "namespacedKey", s -> {
            try {
                return NamespacedKey.fromString(s);
            } catch (Exception e) {
                return null;
            }
        }) {};

        assertThatThrownBy(() -> resolver.resolveDefault(mockContext(), "minecraft:stone!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown namespacedKey");
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
