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
package com.hanielfialho.example.paper;

import static org.assertj.core.api.Assertions.assertThat;

import com.hanielfialho.api.command.CommandAdapterFactory;
import com.hanielfialho.api.command.CommandPath;
import com.hanielfialho.api.executor.CommandExecutor;
import com.hanielfialho.api.result.CommandResult;
import com.hanielfialho.api.source.CommandSource;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

final class ExampleWarpCommandAdapterTest {

    @Test
    void generatedFactoryIsDiscoverable() {
        var loader = ServiceLoader.load(CommandAdapterFactory.class, ExampleWarpCommand.class.getClassLoader());

        var factories = loader.stream()
                .map(ServiceLoader.Provider::get)
                .filter(factory -> factory.supports(new ExampleWarpCommand()))
                .toList();

        assertThat(factories).hasSize(1);
    }

    @Test
    void generatedFactoryCreatesAdapter() {
        var loader = ServiceLoader.load(CommandAdapterFactory.class, ExampleWarpCommand.class.getClassLoader());

        var factory = loader.stream()
                .map(ServiceLoader.Provider::get)
                .filter(f -> f.supports(new ExampleWarpCommand()))
                .findFirst()
                .orElseThrow();

        var adapter = factory.createAdapter(new ExampleWarpCommand(), new ImmediateCommandExecutor());

        assertThat(adapter).isNotNull();
        assertThat(adapter.metadata().name()).isEqualTo("cexample");
    }

    private static final class ImmediateCommandExecutor implements CommandExecutor {
        @Override
        public CommandResult executeSync(CommandSource source, CommandPath path, Runnable command) {
            command.run();
            return CommandResult.success();
        }

        @Override
        public CompletableFuture<CommandResult> executeAsync(CommandSource source, CommandPath path, Runnable command) {
            command.run();
            return CompletableFuture.completedFuture(CommandResult.success());
        }
    }
}
