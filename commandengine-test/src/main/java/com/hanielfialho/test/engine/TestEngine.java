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
package com.hanielfialho.test.engine;

import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.runtime.CommandEngine;
import com.hanielfialho.test.brigadier.LocalBrigadierAdapter;
import com.mojang.brigadier.CommandDispatcher;
import org.jetbrains.annotations.NotNull;

public final class TestEngine {

    private TestEngine() {}

    public static @NotNull CommandEngine create() {
        return harness().engine();
    }

    @SuppressWarnings("resource")
    public static @NotNull Harness harness() {
        var adapter = new LocalBrigadierAdapter();
        var engine = CommandEngine.builder().brigadier(adapter).build();
        return new Harness(engine, adapter.dispatcher());
    }

    public record Harness(
            @NotNull CommandEngine engine, @NotNull CommandDispatcher<CommandSource> dispatcher)
            implements AutoCloseable {

        @Override
        public void close() {
            engine.close();
        }
    }
}
