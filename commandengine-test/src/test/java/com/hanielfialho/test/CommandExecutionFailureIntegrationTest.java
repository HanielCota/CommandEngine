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
package com.hanielfialho.test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hanielfialho.test.command.IntegrationWarpCommand;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.junit.jupiter.api.Test;

final class CommandExecutionFailureIntegrationTest {

    @Test
    void invalidArgumentThrowsSyntaxException() {
        try (var harness = com.hanielfialho.test.engine.TestEngine.harness()) {
            var source = source("tester");
            source.addPermission("warp.use");
            harness.engine().register(new IntegrationWarpCommand());

            assertThatThrownBy(() -> harness.dispatcher().execute("warp teleport", source))
                    .isInstanceOf(CommandSyntaxException.class);
        }
    }

    @Test
    void noPermissionBlocksExecution() {
        try (var harness = com.hanielfialho.test.engine.TestEngine.harness()) {
            var source = source("tester");
            harness.engine().register(new IntegrationWarpCommand());

            assertThatThrownBy(() -> harness.dispatcher().execute("warp teleport spawn", source))
                    .isInstanceOf(CommandSyntaxException.class);
        }
    }

    @Test
    void unknownCommandThrowsException() {
        try (var harness = com.hanielfialho.test.engine.TestEngine.harness()) {
            var source = source("tester");

            assertThatThrownBy(() -> harness.dispatcher().execute("nonexistent", source))
                    .isInstanceOf(CommandSyntaxException.class);
        }
    }

    private static com.hanielfialho.test.source.MockCommandSource source(String name) {
        return new com.hanielfialho.test.source.MockCommandSource(name);
    }
}
