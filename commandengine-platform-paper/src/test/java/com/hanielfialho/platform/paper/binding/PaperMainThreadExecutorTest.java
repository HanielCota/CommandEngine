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
package com.hanielfialho.platform.paper.binding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hanielfialho.platform.paper.scheduler.PaperCommandScheduler;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class PaperMainThreadExecutorTest {

    private Object originalServer;

    @BeforeEach
    void setUp() throws Exception {
        Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        originalServer = serverField.get(null);
    }

    @AfterEach
    void tearDown() throws Exception {
        Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, originalServer);
    }

    @Test
    void taskRunsOnMainThread() throws Exception {
        var server = serverWithScheduler(true);
        setBukkitServer(server);
        var plugin = plugin(true, server);
        var scheduler = new PaperCommandScheduler(plugin);
        var executed = new AtomicBoolean(false);

        scheduler.execute(() -> executed.set(true));

        assertThat(executed).isTrue();
    }

    @Test
    void taskAlreadyOnMainThreadRunsDirectly() throws Exception {
        var schedulerAccessed = new AtomicBoolean(false);
        var server = (Server) Proxy.newProxyInstance(
                Server.class.getClassLoader(), new Class<?>[] {Server.class}, (proxy, method, args) -> {
                    if (method.getName().equals("isPrimaryThread")) return true;
                    if (method.getName().equals("getScheduler")) {
                        schedulerAccessed.set(true);
                        return Proxy.newProxyInstance(
                                org.bukkit.scheduler.BukkitScheduler.class.getClassLoader(),
                                new Class<?>[] {org.bukkit.scheduler.BukkitScheduler.class},
                                (schedProxy, schedMethod, schedArgs) -> {
                                    if (schedMethod.getName().equals("runTask")) {
                                        Runnable task = (Runnable) schedArgs[1];
                                        task.run();
                                        return 1;
                                    }
                                    return null;
                                });
                    }
                    return defaultValue(method.getReturnType());
                });
        setBukkitServer(server);
        var plugin = plugin(true, server);
        var scheduler = new PaperCommandScheduler(plugin);
        var executed = new AtomicBoolean(false);

        scheduler.execute(() -> executed.set(true));

        assertThat(executed).isTrue();
        assertThat(schedulerAccessed).isFalse();
    }

    @Test
    void exceptionPropagated() throws Exception {
        var server = serverWithScheduler(true);
        setBukkitServer(server);
        var plugin = plugin(true, server);
        var scheduler = new PaperCommandScheduler(plugin);

        assertThatThrownBy(() -> scheduler.execute(() -> {
                    throw new IllegalStateException("task error");
                }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("task error");
    }

    @Test
    void executorAfterPluginDisable() throws Exception {
        var server = serverWithScheduler(true);
        setBukkitServer(server);
        var plugin = plugin(false, server);
        var scheduler = new PaperCommandScheduler(plugin);
        var executed = new AtomicBoolean(false);

        scheduler.execute(() -> executed.set(true));

        assertThat(executed).isFalse();
    }

    private static void setBukkitServer(Server server) throws Exception {
        Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, server);
    }

    private static Server serverWithScheduler(boolean primaryThread) {
        return (Server) Proxy.newProxyInstance(
                Server.class.getClassLoader(), new Class<?>[] {Server.class}, (proxy, method, args) -> {
                    if (method.getName().equals("isPrimaryThread")) return primaryThread;
                    if (method.getName().equals("getScheduler")) {
                        return Proxy.newProxyInstance(
                                org.bukkit.scheduler.BukkitScheduler.class.getClassLoader(),
                                new Class<?>[] {org.bukkit.scheduler.BukkitScheduler.class},
                                (schedProxy, schedMethod, schedArgs) -> {
                                    if (schedMethod.getName().equals("runTask")) {
                                        Runnable task = (Runnable) schedArgs[1];
                                        task.run();
                                        return 1;
                                    }
                                    return null;
                                });
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private static Plugin plugin(boolean enabled, Server server) {
        return (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(), new Class<?>[] {Plugin.class}, (proxy, method, args) -> {
                    if (method.getName().equals("isEnabled")) return enabled;
                    if (method.getName().equals("getServer")) return server;
                    if (method.getName().equals("getName")) return "TestPlugin";
                    return defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return null;
    }
}
