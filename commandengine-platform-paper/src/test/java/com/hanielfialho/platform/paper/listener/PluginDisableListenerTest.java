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
        Plugin plugin = pluginProxy("plugin");
        var called = new AtomicBoolean(false);
        var listener = new PluginDisableListener(plugin, () -> called.set(true));

        listener.onPluginDisable(new PluginDisableEvent(plugin));

        assertThat(called).isTrue();
    }

    @Test
    void ignoresDisableEventForOtherPlugins() {
        Plugin plugin = pluginProxy("plugin");
        Plugin other = pluginProxy("other");
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

    private static Plugin pluginProxy(String id) {
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
