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
import java.util.ArrayList;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

final class PaperCommandSourceEdgeTest {

    @Test
    void wrapsConsoleSender() {
        var console = consoleSender();
        var source = new PaperCommandSource(console);
        assertThat(source.getName()).isEqualTo("Console");
        assertThat(source.getHandle()).isSameAs(console);
    }

    @Test
    void delegatesSendMessage() {
        var messages = new ArrayList<String>();
        var sender = (CommandSender) Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(), new Class<?>[] {CommandSender.class}, (proxy, method, args) -> {
                    if (method.getName().equals("sendMessage") && args.length == 1) {
                        messages.add((String) args[0]);
                        return null;
                    }
                    if (method.getName().equals("getName")) return "Console";
                    return defaultReturn(method.getReturnType());
                });
        var source = new PaperCommandSource(sender);
        source.sendMessage("hello");
        assertThat(messages).containsExactly("hello");
    }

    @Test
    void emptyPermissionAlwaysPasses() {
        var console = consoleSender();
        var source = new PaperCommandSource(console);
        assertThat(source.hasPermission("")).isTrue();
    }

    private static CommandSender consoleSender() {
        return (CommandSender) Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(), new Class<?>[] {CommandSender.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getName")) return "Console";
                    if (method.getName().equals("hasPermission")) return true;
                    if (method.getName().equals("sendMessage")) return null;
                    return defaultReturn(method.getReturnType());
                });
    }

    private static Object defaultReturn(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == String.class) return "";
        return null;
    }
}
