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
import org.junit.jupiter.api.Test;

final class RateLimiterKeyTest {

    @Test
    void limitByUser() {
        var limiter = new CaffeineCommandRateLimiter(Duration.ofMinutes(1), 1, 100);
        var source = testSource("player");

        assertThat(limiter.tryAcquire(source, new CommandPath("cmd1"))).isTrue();
        assertThat(limiter.tryAcquire(source, new CommandPath("cmd2"))).isTrue();
        assertThat(limiter.tryAcquire(source, new CommandPath("cmd1"))).isFalse();
        assertThat(limiter.tryAcquire(source, new CommandPath("cmd2"))).isFalse();
    }

    @Test
    void limitByCommand() {
        var limiter = new CaffeineCommandRateLimiter(Duration.ofMinutes(1), 1, 100);
        var path = new CommandPath("cmd");

        assertThat(limiter.tryAcquire(testSource("alice"), path)).isTrue();
        assertThat(limiter.tryAcquire(testSource("bob"), path)).isTrue();
        assertThat(limiter.tryAcquire(testSource("alice"), path)).isFalse();
        assertThat(limiter.tryAcquire(testSource("bob"), path)).isFalse();
    }

    @Test
    void limitByUserAndCommand() {
        var limiter = new CaffeineCommandRateLimiter(Duration.ofMinutes(1), 1, 100);
        var source = testSource("player");

        assertThat(limiter.tryAcquire(source, new CommandPath("a"))).isTrue();
        assertThat(limiter.tryAcquire(source, new CommandPath("a"))).isFalse();
        assertThat(limiter.tryAcquire(source, new CommandPath("b"))).isTrue();
        assertThat(limiter.tryAcquire(source, new CommandPath("b"))).isFalse();
    }

    @Test
    void sourceWithoutId() {
        var limiter = new CaffeineCommandRateLimiter(Duration.ofMinutes(1), 2, 100);
        var source = testSource("");

        assertThat(limiter.tryAcquire(source, new CommandPath("cmd"))).isTrue();
        assertThat(limiter.tryAcquire(source, new CommandPath("cmd"))).isTrue();
        assertThat(limiter.tryAcquire(source, new CommandPath("cmd"))).isFalse();
    }

    @Test
    void keyIsStable() {
        var limiter = new CaffeineCommandRateLimiter(Duration.ofMinutes(1), 2, 100);
        var path = new CommandPath("test");
        var source = testSource("stable");

        assertThat(limiter.tryAcquire(source, path)).isTrue();
        assertThat(limiter.tryAcquire(source, path)).isTrue();
        assertThat(limiter.tryAcquire(source, path)).isFalse();
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
            public void sendMessage(String message) {}

            @Override
            public String getName() {
                return name;
            }
        };
    }
}
