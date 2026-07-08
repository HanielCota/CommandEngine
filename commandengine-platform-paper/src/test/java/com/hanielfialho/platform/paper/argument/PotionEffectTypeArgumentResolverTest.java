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
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;

final class PotionEffectTypeArgumentResolverTest {

    @Test
    void validEffect() {
        var captured = new AtomicReference<String>();
        var resolver =
                new AbstractPaperArgumentResolver<PotionEffectType>(PotionEffectType.class, "potionEffect", name -> {
                    captured.set(name);
                    return null;
                }) {};

        assertThatThrownBy(() -> resolver.resolveDefault(mockContext(), "speed"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(captured.get()).isEqualTo("speed");
    }

    @Test
    void invalidEffect() {
        var resolver = new AbstractPaperArgumentResolver<PotionEffectType>(
                PotionEffectType.class, "potionEffect", name -> null) {};
        assertThatThrownBy(() -> resolver.resolveDefault(mockContext(), "unknown_effect"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown potionEffect: unknown_effect");
    }

    @Test
    void namespacedKey() {
        var captured = new AtomicReference<String>();
        var resolver =
                new AbstractPaperArgumentResolver<PotionEffectType>(PotionEffectType.class, "potionEffect", name -> {
                    captured.set(name);
                    return null;
                }) {};

        assertThatThrownBy(() -> resolver.resolveDefault(mockContext(), "minecraft:speed"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(captured.get()).isEqualTo("minecraft:speed");
    }

    @Test
    void suggestion() {
        var resolver = new AbstractPaperArgumentResolver<PotionEffectType>(
                PotionEffectType.class, "potionEffect", name -> null) {};
        assertThat(resolver.supportsDefault()).isTrue();
        assertThat(resolver.type()).isEqualTo(PotionEffectType.class);
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
