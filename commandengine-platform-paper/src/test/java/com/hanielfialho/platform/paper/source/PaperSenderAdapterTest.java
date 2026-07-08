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
package com.hanielfialho.platform.paper.source;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

final class PaperSenderAdapterTest {

    @Test
    void playerSender() {
        var player = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(), new Class<?>[] {Player.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getName")) return "haniel";
                    if (method.getName().equals("hasPermission")) return true;
                    return defaultReturn(method.getReturnType());
                });
        var source = new PaperCommandSource(player);
        assertThat(source.getName()).isEqualTo("haniel");
        assertThat(source.hasPermission("test.perm")).isTrue();
        assertThat(source.getHandle()).isSameAs(player);
    }

    @Test
    void consoleSender() {
        var console = (ConsoleCommandSender) Proxy.newProxyInstance(
                ConsoleCommandSender.class.getClassLoader(),
                new Class<?>[] {ConsoleCommandSender.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getName")) return "Console";
                    if (method.getName().equals("hasPermission")) return true;
                    return defaultReturn(method.getReturnType());
                });
        var source = new PaperCommandSource(console);
        assertThat(source.getName()).isEqualTo("Console");
        assertThat(source.getHandle()).isSameAs(console);
    }

    @Test
    void remoteConsole() {
        var remote = (RemoteConsoleCommandSender) Proxy.newProxyInstance(
                RemoteConsoleCommandSender.class.getClassLoader(),
                new Class<?>[] {RemoteConsoleCommandSender.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getName")) return "RCON";
                    if (method.getName().equals("hasPermission")) return true;
                    return defaultReturn(method.getReturnType());
                });
        var source = new PaperCommandSource(remote);
        assertThat(source.getName()).isEqualTo("RCON");
        assertThat(source.getHandle()).isSameAs(remote);
    }

    @Test
    void commandBlock() {
        var cmdBlock = (BlockCommandSender) Proxy.newProxyInstance(
                BlockCommandSender.class.getClassLoader(),
                new Class<?>[] {BlockCommandSender.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getName")) return "@";
                    if (method.getName().equals("hasPermission")) return true;
                    return defaultReturn(method.getReturnType());
                });
        var source = new PaperCommandSource(cmdBlock);
        assertThat(source.getName()).isEqualTo("@");
        assertThat(source.getHandle()).isSameAs(cmdBlock);
    }

    @Test
    void entitySender() {
        var sender = (CommandSender) Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(), new Class<?>[] {CommandSender.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getName")) return "entity";
                    if (method.getName().equals("hasPermission")) return false;
                    return defaultReturn(method.getReturnType());
                });
        var source = new PaperCommandSource(sender);
        assertThat(source.getName()).isEqualTo("entity");
        assertThat(source.hasPermission("some.perm")).isFalse();
    }

    private static Object defaultReturn(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return null;
    }
}
