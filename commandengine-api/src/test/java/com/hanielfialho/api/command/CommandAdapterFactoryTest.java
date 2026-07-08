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
package com.hanielfialho.api.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hanielfialho.api.argument.ArgumentResolverRegistry;
import com.hanielfialho.api.executor.CommandExecutor;
import com.hanielfialho.api.message.CommandMessages;
import com.hanielfialho.api.rate.CommandRateLimiter;
import com.hanielfialho.api.registry.BrigadierAdapter;
import com.hanielfialho.api.result.CommandResult;
import com.hanielfialho.api.scheduler.CommandScheduler;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.api.suggestion.SuggestionExecutor;
import com.hanielfialho.api.telemetry.CommandTelemetry;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

final class CommandAdapterFactoryTest {

    @Test
    void createAdapterWithNullArgumentResolversUsesEmptyRegistry() {
        var factory = new TestCommandAdapterFactory();

        CommandAdapter adapter = factory.createAdapter(new TestCommand(), new TestExecutor(), null);

        assertThat(adapter).isNotNull();
    }

    @Test
    void createAdapterWithNullInstanceThrowsNullPointerException() {
        var factory = new TestCommandAdapterFactory();

        assertThatThrownBy(() -> factory.createAdapter(null, new TestExecutor()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("instance");
    }

    private static final class TestCommand {}

    private static final class TestCommandAdapter implements CommandAdapter {

        @Override
        public CommandMetadata metadata() {
            return new CommandMetadata("test", java.util.List.of(), "", "", java.util.List.of());
        }

        @Override
        public void register(BrigadierAdapter brigadier) {}

        @Override
        public void unregister(BrigadierAdapter brigadier) {}
    }

    private static final class TestCommandAdapterFactory implements CommandAdapterFactory<TestCommand> {

        @Override
        public Class<TestCommand> type() {
            return TestCommand.class;
        }

        @Override
        public CommandAdapter create(TestCommand instance, CommandExecutor executor) {
            return new TestCommandAdapter();
        }

        @Override
        public CommandAdapter create(
                TestCommand instance,
                CommandExecutor executor,
                ArgumentResolverRegistry argumentResolvers,
                CommandScheduler scheduler,
                CommandMessages messages,
                CommandTelemetry telemetry,
                CommandRateLimiter rateLimiter,
                SuggestionExecutor suggestionExecutor) {
            assertThat(argumentResolvers).isNotNull();
            return new TestCommandAdapter();
        }
    }

    private static final class TestExecutor implements CommandExecutor {

        @Override
        public CommandResult executeSync(CommandSource source, CommandPath path, Runnable command) {
            return CommandResult.success();
        }

        @Override
        public CompletableFuture<CommandResult> executeAsync(CommandSource source, CommandPath path, Runnable command) {
            return CompletableFuture.completedFuture(CommandResult.success());
        }
    }
}
