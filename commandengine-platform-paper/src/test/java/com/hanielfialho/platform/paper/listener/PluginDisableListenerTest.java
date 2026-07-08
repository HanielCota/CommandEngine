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
package com.hanielfialho.platform.paper.listener;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class PluginDisableListenerTest {

    private Object originalServer;

    @BeforeEach
    void setUp() throws Exception {
        Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        originalServer = serverField.get(null);
        serverField.set(null, serverProxy());
    }

    @AfterEach
    void tearDown() throws Exception {
        Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, originalServer);
    }

    @Test
    void invokesCallbackWhenMatchingPluginIsDisabled() {
        Plugin plugin = pluginProxy();
        var called = new AtomicBoolean(false);
        var listener = new PluginDisableListener(plugin, () -> called.set(true));

        listener.onPluginDisable(new PluginDisableEvent(plugin));

        assertThat(called).isTrue();
    }

    @Test
    void ignoresDisableEventForOtherPlugins() {
        Plugin plugin = pluginProxy();
        Plugin other = pluginProxy();
        var called = new AtomicBoolean(false);
        var listener = new PluginDisableListener(plugin, () -> called.set(true));

        listener.onPluginDisable(new PluginDisableEvent(other));

        assertThat(called).isFalse();
    }

    private static Server serverProxy() {
        return (Server) Proxy.newProxyInstance(
                Server.class.getClassLoader(), new Class<?>[] {Server.class}, (proxy, method, args) -> {
                    if ("isPrimaryThread".equals(method.getName())) {
                        return true;
                    }
                    return null;
                });
    }

    private static Plugin pluginProxy() {
        return (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(), new Class<?>[] {Plugin.class}, (proxy, method, args) -> {
                    if ("equals".equals(method.getName()) && args != null && args.length == 1) {
                        return proxy == args[0];
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    return null;
                });
    }
}
