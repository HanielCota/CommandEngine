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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hanielfialho.runtime.internal.argument.DurationArgumentResolver;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class DurationArgumentResolverTest {

    private final DurationArgumentResolver resolver = new DurationArgumentResolver();

    @Test
    void typeIsDuration() {
        assertThat(resolver.type()).isEqualTo(Duration.class);
    }

    @Test
    void resolvesSeconds() throws Exception {
        var captured = new AtomicReference<Duration>();
        execute("cmd 10s", captured);
        assertThat(captured.get()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void resolvesMinutes() throws Exception {
        var captured = new AtomicReference<Duration>();
        execute("cmd 5m", captured);
        assertThat(captured.get()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void resolvesHours() throws Exception {
        var captured = new AtomicReference<Duration>();
        execute("cmd 2h", captured);
        assertThat(captured.get()).isEqualTo(Duration.ofHours(2));
    }

    @Test
    void resolvesDays() throws Exception {
        var captured = new AtomicReference<Duration>();
        execute("cmd 1d", captured);
        assertThat(captured.get()).isEqualTo(Duration.ofDays(1));
    }

    @Test
    void throwsForInvalidFormat() {
        assertThatThrownBy(() -> execute("cmd abc", new AtomicReference<>()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throwsForNegativeDuration() throws Exception {
        var captured = new AtomicReference<Duration>();
        execute("cmd -5m", captured);
        assertThat(captured.get()).isEqualTo(Duration.ofMinutes(-5));
    }

    private void execute(String input, AtomicReference<Duration> captured) throws Exception {
        var dispatcher = new CommandDispatcher<Object>();
        dispatcher.register(LiteralArgumentBuilder.<Object>literal("cmd")
                .then(RequiredArgumentBuilder.argument("dur", resolver.argumentType())
                        .executes(ctx -> {
                            captured.set(resolver.resolve(ctx, "dur"));
                            return 1;
                        })));
        dispatcher.execute(input, new Object());
    }
}
