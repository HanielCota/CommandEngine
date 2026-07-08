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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

final class PaperCommandSourceTest {

    private static CommandSender senderProxy(boolean hasPerm, String name) {
        return (CommandSender) Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(), new Class<?>[] {CommandSender.class}, (proxy, method, args) -> {
                    var methodName = method.getName();
                    return switch (methodName) {
                        case "hasPermission" -> hasPerm;
                        case "getName" -> name;
                        case "sendMessage" -> null;
                        default -> defaultValue(method.getReturnType());
                    };
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (type.isPrimitive()) {
            if (type == boolean.class) return false;
            if (type == int.class) return 0;
            if (type == long.class) return 0L;
        }
        return null;
    }

    @Test
    void hasPermissionReturnsTrueWhenSenderHasPermission() {
        var sender = new PaperCommandSource(senderProxy(true, "player"));
        assertThat(sender.hasPermission("some.perm")).isTrue();
    }

    @Test
    void hasPermissionReturnsFalseWhenSenderLacksPermission() {
        var sender = new PaperCommandSource(senderProxy(false, "player"));
        assertThat(sender.hasPermission("some.perm")).isFalse();
    }

    @Test
    void hasPermissionReturnsTrueForEmptyPermission() {
        var sender = new PaperCommandSource(senderProxy(false, "player"));
        assertThat(sender.hasPermission("")).isTrue();
    }

    @Test
    void hasPermissionThrowsOnNull() {
        var sender = new PaperCommandSource(senderProxy(true, "player"));
        assertThatThrownBy(() -> sender.hasPermission(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("permission");
    }

    @Test
    void getHandleReturnsCommandSender() {
        var handle = senderProxy(true, "player");
        var sender = new PaperCommandSource(handle);
        assertThat(sender.getHandle()).isSameAs(handle);
    }

    @Test
    void getNameReturnsSenderName() {
        var sender = new PaperCommandSource(senderProxy(true, "TestPlayer"));
        assertThat(sender.getName()).isEqualTo("TestPlayer");
    }

    @Test
    void sendMessageDelegatesToSender() {
        var sender = new PaperCommandSource(senderProxy(true, "player"));
        sender.sendMessage("hello");
    }

    @Test
    void sendMessageThrowsOnNull() {
        var sender = new PaperCommandSource(senderProxy(true, "player"));
        assertThatThrownBy(() -> sender.sendMessage(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("message");
    }

    @Test
    void throwsOnNullHandle() {
        assertThatThrownBy(() -> new PaperCommandSource(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("handle");
    }
}
