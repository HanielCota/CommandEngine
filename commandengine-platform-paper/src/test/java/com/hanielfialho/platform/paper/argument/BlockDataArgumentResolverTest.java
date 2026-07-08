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

import java.lang.reflect.Proxy;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.junit.jupiter.api.Test;

final class BlockDataArgumentResolverTest {

    @Test
    void simpleBlockData() {
        var blockData = blockDataProxy("minecraft:stone");
        var resolver = new AbstractPaperArgumentResolver<BlockData>(BlockData.class, "blockData", name -> blockData) {};

        var result = resolver.resolveDefault(mockContext(), "minecraft:stone");

        assertThat(result).isSameAs(blockData);
    }

    @Test
    void blockDataWithProperties() {
        var blockData = blockDataProxy("minecraft:oak_log[axis=y]");
        var resolver = new AbstractPaperArgumentResolver<BlockData>(BlockData.class, "blockData", name -> blockData) {};

        var result = resolver.resolveDefault(mockContext(), "minecraft:oak_log[axis=y]");

        assertThat(result).isSameAs(blockData);
    }

    @Test
    void invalidProperty() {
        var resolver = new AbstractPaperArgumentResolver<BlockData>(BlockData.class, "blockData", name -> null) {};

        assertThatThrownBy(() -> resolver.resolveDefault(mockContext(), "minecraft:stone[invalid=bad]"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown blockData");
    }

    @Test
    void nonBlockMaterial() {
        var resolver = new AbstractPaperArgumentResolver<BlockData>(BlockData.class, "blockData", name -> null) {};

        assertThatThrownBy(() -> resolver.resolveDefault(mockContext(), "minecraft:air"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown blockData");
    }

    private static com.mojang.brigadier.context.CommandContext<?> mockContext() {
        return new com.mojang.brigadier.context.CommandContextBuilder(
                        new com.mojang.brigadier.CommandDispatcher<>(),
                        new Object(),
                        new com.mojang.brigadier.tree.RootCommandNode<>(),
                        0)
                .build("test");
    }

    private static BlockData blockDataProxy(String asString) {
        return (BlockData) Proxy.newProxyInstance(
                BlockData.class.getClassLoader(), new Class<?>[] {BlockData.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getAsString")) return asString;
                    if (method.getName().equals("getMaterial")) return Material.STONE;
                    return null;
                });
    }
}
