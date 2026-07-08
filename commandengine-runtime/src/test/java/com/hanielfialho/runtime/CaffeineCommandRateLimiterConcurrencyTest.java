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
package com.hanielfialho.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.hanielfialho.api.command.CommandPath;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.runtime.internal.rate.CaffeineCommandRateLimiter;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class CaffeineCommandRateLimiterConcurrencyTest {

    @Test
    void concurrentAccessDoesNotExceedLimit() throws Exception {
        var limiter = new CaffeineCommandRateLimiter(Duration.ofSeconds(10), 5, 1000);
        var path = new CommandPath("test");
        var source = testSource("concurrent-user");
        var threads = 10;
        var pool = Executors.newFixedThreadPool(threads);
        var allowed = new AtomicInteger();
        var latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                if (limiter.tryAcquire(source, path)) {
                    allowed.incrementAndGet();
                }
                latch.countDown();
            });
        }
        latch.await();
        pool.shutdown();

        assertThat(allowed.get()).isLessThanOrEqualTo(5);
    }

    @Test
    void bypassPermissionSkipsLimit() {
        var limiter = new CaffeineCommandRateLimiter(Duration.ofSeconds(10), 1, 1000);
        var path = new CommandPath("test");
        var admin = new CommandSource() {
            @Override
            public boolean hasPermission(String permission) {
                return true;
            }

            @Override
            public Object getHandle() {
                return this;
            }

            @Override
            public void sendMessage(String message) {
                // no-op: test stub
            }

            @Override
            public String getName() {
                return "admin";
            }
        };
        assertThat(limiter.tryAcquire(admin, path)).isTrue();
        assertThat(limiter.tryAcquire(admin, path)).isTrue();
    }

    @Test
    void differentPathsHaveIndependentCounters() {
        var limiter = new CaffeineCommandRateLimiter(Duration.ofSeconds(10), 1, 1000);
        var source = testSource("multi-path");
        assertThat(limiter.tryAcquire(source, new CommandPath("cmd1"))).isTrue();
        assertThat(limiter.tryAcquire(source, new CommandPath("cmd1"))).isFalse();
        assertThat(limiter.tryAcquire(source, new CommandPath("cmd2"))).isTrue();
    }

    private static CommandSource testSource(String name) {
        return new CommandSource() {
            @Override
            public boolean hasPermission(String permission) {
                return false;
            }

            @Override
            public Object getHandle() {
                return this;
            }

            @Override
            public void sendMessage(String message) {
                // no-op: test stub
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }
}
