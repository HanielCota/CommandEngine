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

final class PaperAudienceMessageTest {

    @Test
    void legacyMessage() {
        var messages = new ArrayList<String>();
        var sender = senderWithCapture(messages);
        var source = new PaperCommandSource(sender);

        source.sendMessage("&cHello World");

        assertThat(messages).containsExactly("&cHello World");
    }

    @Test
    void adventureComponent() {
        var messages = new ArrayList<String>();
        var sender = senderWithCapture(messages);
        var source = new PaperCommandSource(sender);

        source.sendMessage("<red>Hello World");

        assertThat(messages).containsExactly("<red>Hello World");
    }

    @Test
    void emptyMessage() {
        var messages = new ArrayList<String>();
        var sender = senderWithCapture(messages);
        var source = new PaperCommandSource(sender);

        source.sendMessage("");

        assertThat(messages).containsExactly("");
    }

    @Test
    void colorFormattingPreserved() {
        var messages = new ArrayList<String>();
        var sender = senderWithCapture(messages);
        var source = new PaperCommandSource(sender);

        source.sendMessage("§a§lBold Green");

        assertThat(messages).containsExactly("§a§lBold Green");
    }

    private static CommandSender senderWithCapture(ArrayList<String> messages) {
        return (CommandSender) Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(), new Class<?>[] {CommandSender.class}, (proxy, method, args) -> {
                    if (method.getName().equals("sendMessage") && args.length == 1) {
                        messages.add((String) args[0]);
                        return null;
                    }
                    if (method.getName().equals("getName")) return "tester";
                    if (method.getName().equals("hasPermission")) return true;
                    return defaultReturn(method.getReturnType());
                });
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
