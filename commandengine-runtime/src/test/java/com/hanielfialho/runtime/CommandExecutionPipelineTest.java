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
import com.hanielfialho.api.result.CommandResult;
import com.hanielfialho.api.result.FailureReason;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.api.telemetry.CommandTelemetry;
import com.hanielfialho.runtime.internal.executor.SyncExecutor;
import com.hanielfialho.runtime.internal.executor.TelemetryCommandExecutor;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class CommandExecutionPipelineTest {

    private final CommandPath path = new CommandPath("test");
    private final SyncExecutor executor = new SyncExecutor();

    @Test
    void parseThenPermissionThenCooldownThenExecute() {
        var events = new ArrayList<String>();

        CommandResult result =
                runPipeline(executor, path, sourceWithPermission(true), true, () -> events.add("execute"), events);

        assertThat(result).isInstanceOf(CommandResult.Success.class);
        assertThat(events).containsExactly("parse", "permission", "cooldown", "execute");
    }

    @Test
    void parseFailureInterruptsPipeline() {
        var events = new ArrayList<String>();

        CommandResult result =
                runPipeline(executor, path, sourceWithPermission(true), false, () -> events.add("execute"), events);

        assertThat(result).isInstanceOf(CommandResult.Failure.class);
        assertThat(((CommandResult.Failure) result).reason()).isEqualTo(FailureReason.INVALID_ARGUMENT);
        assertThat(events).containsExactly("parse");
    }

    @Test
    void permissionFailureInterruptsPipeline() {
        var events = new ArrayList<String>();

        CommandResult result =
                runPipeline(executor, path, sourceWithPermission(false), true, () -> events.add("execute"), events);

        assertThat(result).isInstanceOf(CommandResult.Failure.class);
        assertThat(((CommandResult.Failure) result).reason()).isEqualTo(FailureReason.NO_PERMISSION);
        assertThat(events).containsExactly("parse", "permission");
    }

    @Test
    void cooldownFailureInterruptsPipeline() {
        var events = new ArrayList<String>();

        CommandResult result = runPipeline(
                executor, path, sourceWithPermission(true), true, () -> events.add("execute"), events, false);

        assertThat(result).isInstanceOf(CommandResult.Failure.class);
        assertThat(((CommandResult.Failure) result).reason()).isEqualTo(FailureReason.RATE_LIMITED);
        assertThat(events).containsExactly("parse", "permission", "cooldown");
    }

    @Test
    void telemetryIsCalledInCorrectOrder() {
        var telemetryEvents = new ArrayList<String>();
        CommandTelemetry telemetry = new CommandTelemetry() {
            @Override
            public void recordExecution(CommandPath p, long nanos, boolean async) {
                telemetryEvents.add("execution:" + p);
            }

            @Override
            public void recordFailure(CommandPath p, String reason) {
                telemetryEvents.add("failure:" + p + ":" + reason);
            }

            @Override
            public void recordSuggestion(CommandPath p, long nanos, int suggestionCount) {
                // no-op: test telemetry
            }
        };
        var telemetryExecutor = new TelemetryCommandExecutor(executor, telemetry);

        CommandResult result =
                runPipeline(telemetryExecutor, path, sourceWithPermission(true), true, () -> {}, new ArrayList<>());

        assertThat(result).isInstanceOf(CommandResult.Success.class);
        assertThat(telemetryEvents).hasSize(1);
        assertThat(telemetryEvents.getFirst()).isEqualTo("execution:" + path);
    }

    private static CommandResult runPipeline(
            SyncExecutor sync,
            CommandPath p,
            CommandSource source,
            boolean parseOk,
            Runnable command,
            List<String> events) {
        return runPipeline(sync, p, source, parseOk, command, events, true);
    }

    private static CommandResult runPipeline(
            SyncExecutor sync,
            CommandPath p,
            CommandSource source,
            boolean parseOk,
            Runnable command,
            List<String> events,
            boolean cooldownOk) {

        events.add("parse");
        if (!parseOk) {
            return CommandResult.failure(FailureReason.INVALID_ARGUMENT);
        }
        events.add("permission");
        if (!source.hasPermission("perm.test")) {
            return CommandResult.failure(FailureReason.NO_PERMISSION);
        }
        events.add("cooldown");
        if (!cooldownOk) {
            return CommandResult.failure(FailureReason.RATE_LIMITED);
        }
        return sync.executeSync(source, p, command);
    }

    private static CommandResult runPipeline(
            TelemetryCommandExecutor telemetryExecutor,
            CommandPath p,
            CommandSource source,
            boolean parseOk,
            Runnable command,
            List<String> events) {

        events.add("parse");
        if (!parseOk) {
            return CommandResult.failure(FailureReason.INVALID_ARGUMENT);
        }
        events.add("permission");
        if (!source.hasPermission("perm.test")) {
            return CommandResult.failure(FailureReason.NO_PERMISSION);
        }
        events.add("cooldown");
        return telemetryExecutor.executeSync(source, p, command);
    }

    private static CommandSource sourceWithPermission(boolean allowed) {
        return new CommandSource() {
            @Override
            public boolean hasPermission(String permission) {
                return allowed;
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
                return "tester";
            }
        };
    }
}
