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

import com.hanielfialho.api.message.CommandMessages;
import com.hanielfialho.api.result.CommandResult;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.runtime.CommandEngine;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

final class PaperAsyncSchedulerExecutorTest {

    private static final CommandMessages MESSAGES = CommandMessages.defaults();

    @Test
    void asyncTask() throws Exception {
        var executor = CommandEngine.virtualThreadExecutor(MESSAGES, Duration.ofSeconds(5));
        var executed = new CountDownLatch(1);
        var result = new AtomicReference<String>();

        executor.executeAsync(source("tester"), () -> {
                    result.set("done");
                    executed.countDown();
                })
                .get(2, TimeUnit.SECONDS);

        assertThat(result.get()).isEqualTo("done");
    }

    @Test
    void asyncError() throws Exception {
        var executor = CommandEngine.virtualThreadExecutor(MESSAGES, Duration.ofSeconds(5));
        var source = source("tester");

        var future = executor.executeAsync(source, () -> {
            throw new RuntimeException("async failure");
        });

        CommandResult result = future.get(2, TimeUnit.SECONDS);
        assertThat(result).isInstanceOf(CommandResult.Failure.class);
    }

    @Test
    void cancellation() throws Exception {
        var executor = CommandEngine.virtualThreadExecutor(MESSAGES, Duration.ofSeconds(10));
        var source = source("tester");
        var executed = new AtomicBoolean(false);

        var future = executor.executeAsync(source, () -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            executed.set(true);
        });

        future.cancel(true);
        assertThat(future.isCancelled()).isTrue();
    }

    @Test
    void pluginDisabled() throws Exception {
        var executor = CommandEngine.virtualThreadExecutor(MESSAGES, Duration.ofMillis(100));
        var source = source("tester");
        var executed = new CountDownLatch(1);

        executor.executeAsync(source, () -> {
                    executed.countDown();
                })
                .get(2, TimeUnit.SECONDS);

        assertThat(executed.await(1, TimeUnit.SECONDS)).isTrue();
    }

    private static CommandSource source(String name) {
        var sender = (CommandSender) Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(), new Class<?>[] {CommandSender.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getName")) return name;
                    if (method.getName().equals("hasPermission")) return true;
                    if (method.getName().equals("sendMessage")) return null;
                    return defaultReturn(method.getReturnType());
                });
        return new com.hanielfialho.platform.paper.source.PaperCommandSource(sender);
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
