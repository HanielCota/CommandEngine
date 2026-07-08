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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hanielfialho.api.command.CommandPath;
import com.hanielfialho.api.executor.CommandExecutor;
import com.hanielfialho.api.rate.CommandRateLimiter;
import com.hanielfialho.api.registry.BrigadierAdapter;
import com.hanielfialho.api.result.CommandResult;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.api.telemetry.CommandTelemetry;
import com.mojang.brigadier.CommandDispatcher;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

final class CommandEngineBuilderTest {

    @Test
    void throwsWhenBrigadierIsMissing() {
        var builder = CommandEngine.builder();
        assertThatThrownBy(builder::build)
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("brigadier");
    }

    @Test
    void throwsOnNullBrigadier() {
        assertThatThrownBy(() -> CommandEngine.builder().brigadier(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void throwsOnNullExecutor() {
        assertThatThrownBy(() -> CommandEngine.builder().executor(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void throwsOnNullRegistry() {
        assertThatThrownBy(() -> CommandEngine.builder().registry(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void throwsOnNullScheduler() {
        assertThatThrownBy(() -> CommandEngine.builder().scheduler(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void throwsOnNullMessages() {
        assertThatThrownBy(() -> CommandEngine.builder().messages(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void throwsOnNullTelemetry() {
        assertThatThrownBy(() -> CommandEngine.builder().telemetry(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void throwsOnNullRateLimiter() {
        assertThatThrownBy(() -> CommandEngine.builder().rateLimiter(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void throwsOnNullSuggestionExecutor() {
        assertThatThrownBy(() -> CommandEngine.builder().suggestionExecutor(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throwsOnNullOwner() {
        assertThatThrownBy(() -> CommandEngine.builder().owner(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void throwsOnNullArgumentResolvers() {
        assertThatThrownBy(() -> CommandEngine.builder().argumentResolvers(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throwsOnNullArgumentResolver() {
        assertThatThrownBy(() -> CommandEngine.builder().argumentResolver(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void throwsOnNullConfig() {
        assertThatThrownBy(() -> CommandEngine.builder().config(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void throwsOnNegativeAsyncTimeout() {
        assertThatThrownBy(() -> CommandEngine.builder().asyncTimeout(Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throwsOnZeroAsyncTimeout() {
        assertThatThrownBy(() -> CommandEngine.builder().asyncTimeout(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throwsOnNullAsyncTimeout() {
        assertThatThrownBy(() -> CommandEngine.builder().asyncTimeout(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void buildsWithMinimalConfiguration() {
        var engine =
                CommandEngine.builder().brigadier(new TestBrigadierAdapter()).build();
        engine.close();
    }

    @Test
    void buildsWithCustomExecutor() {
        var engine = CommandEngine.builder()
                .brigadier(new TestBrigadierAdapter())
                .executor(new NoOpExecutor())
                .build();
        engine.close();
    }

    @Test
    void buildsWithCustomTelemetry() {
        var engine = CommandEngine.builder()
                .brigadier(new TestBrigadierAdapter())
                .telemetry(new CommandTelemetry() {
                    @Override
                    public void recordExecution(
                            com.hanielfialho.api.command.CommandPath path, long nanos, boolean async) {
                        // no-op: test telemetry
                    }

                    @Override
                    public void recordFailure(com.hanielfialho.api.command.CommandPath path, String reason) {
                        // no-op: test telemetry
                    }

                    @Override
                    public void recordSuggestion(
                            com.hanielfialho.api.command.CommandPath path, long nanos, int suggestionCount) {
                        // no-op: test telemetry
                    }
                })
                .build();
        engine.close();
    }

    @Test
    void buildsWithRateLimiter() {
        var engine = CommandEngine.builder()
                .brigadier(new TestBrigadierAdapter())
                .rateLimiter(CommandRateLimiter.NONE)
                .build();
        engine.close();
    }

    private static final class NoOpExecutor implements CommandExecutor {

        @Override
        public CommandResult executeSync(CommandSource source, CommandPath path, Runnable command) {
            command.run();
            return CommandResult.success();
        }

        @Override
        public CompletableFuture<CommandResult> executeAsync(CommandSource source, CommandPath path, Runnable command) {
            return CompletableFuture.completedFuture(executeSync(source, path, command));
        }
    }

    private static final class TestBrigadierAdapter implements BrigadierAdapter {

        private final CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher<>();

        @Override
        public CommandDispatcher<CommandSource> getDispatcher() {
            return dispatcher;
        }

        @Override
        public void unregister(String name) {
            TestBrigadierRootMutator.remove(dispatcher, name);
        }
    }
}
