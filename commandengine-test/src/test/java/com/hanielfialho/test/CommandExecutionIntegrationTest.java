package com.hanielfialho.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hanielfialho.api.argument.ArgumentTypeResolver;
import com.hanielfialho.api.command.CommandPath;
import com.hanielfialho.api.message.CommandMessages;
import com.hanielfialho.api.rate.CommandRateLimiter;
import com.hanielfialho.api.telemetry.CommandTelemetry;
import com.hanielfialho.runtime.CommandEngine;
import com.hanielfialho.test.brigadier.LocalBrigadierAdapter;
import com.hanielfialho.test.command.IntegrationDebugCommand;
import com.hanielfialho.test.command.IntegrationEconomyCommand;
import com.hanielfialho.test.command.IntegrationPlayerOnlyCommand;
import com.hanielfialho.test.command.IntegrationStressCommand;
import com.hanielfialho.test.command.IntegrationTimeCommand;
import com.hanielfialho.test.command.IntegrationWarpCommand;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;

final class CommandExecutionIntegrationTest {

    @Test
    void executesAliasSuggestionsGreedyRangeAndFlagsThroughBrigadier() throws Exception {
        var harness = TestEngine.harness();
        var source = new MockCommandSource("tester");
        source.addPermission("warp.use");
        var warp = new IntegrationWarpCommand();
        var economy = new IntegrationEconomyCommand();
        var debug = new IntegrationDebugCommand();

        harness.engine().register(warp).register(economy).register(debug);

        harness.dispatcher().execute("w teleport spawn", source);
        harness.dispatcher().execute("warp broadcast hello brave world", source);
        harness.dispatcher().execute("warp args alpha beta gamma", source);
        harness.dispatcher().execute("warp list one two", source);
        harness.dispatcher().execute("warp args", source);
        harness.dispatcher().execute("warp root alpha", source);
        harness.dispatcher().execute("w", source);
        harness.dispatcher().execute("eco pay 100", source);
        harness.dispatcher().execute("debug toggle --verbose", source);
        harness.dispatcher().execute("debug multi --beta --alpha", source);

        waitUntil(() -> warp.events().size() == 7
                && economy.events().size() == 1
                && debug.events().size() == 2);

        assertThat(warp.events())
                .containsExactlyInAnyOrder(
                        "teleport:spawn:tester",
                        "broadcast:hello brave world",
                        "args:alpha,beta,gamma",
                        "list:one,two",
                        "args:",
                        "root:tester:root,alpha",
                        "root:tester:");
        assertThat(economy.events()).containsExactly("pay:100");
        assertThat(debug.events()).containsExactlyInAnyOrder("verbose:true", "multi:true:true");

        var parse = harness.dispatcher().parse("warp teleport s", source);
        var suggestions = harness.dispatcher().getCompletionSuggestions(parse).join();
        assertThat(suggestions.getList()).extracting(Suggestion::getText).containsExactlyInAnyOrder("spawn", "shop");

        assertThatThrownBy(() -> harness.dispatcher().execute("eco pay 101", source))
                .isInstanceOf(CommandSyntaxException.class);
        assertThat(economy.events()).containsExactly("pay:100");
    }

    @Test
    void inheritedPermissionBlocksSubcommandBeforeHandlerRuns() {
        var harness = TestEngine.harness();
        var source = new MockCommandSource("tester");
        var warp = new IntegrationWarpCommand();
        harness.engine().register(warp);

        assertThatThrownBy(() -> harness.dispatcher().execute("warp teleport spawn", source))
                .isInstanceOf(CommandSyntaxException.class);
        assertThat(warp.events()).isEmpty();
    }

    @Test
    void resolvesExternalArgumentTypesThroughRegisteredResolvers() throws Exception {
        var adapter = new LocalBrigadierAdapter();
        var engine = CommandEngine.builder()
                .brigadier(adapter)
                .argumentResolver(new InstantArgumentResolver())
                .build();
        var source = new MockCommandSource("tester");
        var command = new IntegrationTimeCommand();

        engine.register(command);
        adapter.dispatcher().execute("time parse 2026-06-18T12:00:00Z", source);

        waitUntil(() -> command.events().size() == 1);

        assertThat(command.events()).containsExactly("instant:2026-06-18T12:00:00Z");
    }

    @Test
    void unregistersPreviouslyRegisteredCommandInstance() {
        var harness = TestEngine.harness();
        var source = new MockCommandSource("tester");
        source.addPermission("warp.use");
        var warp = new IntegrationWarpCommand();
        harness.engine().register(warp);

        harness.engine().unregister(warp);

        assertThatThrownBy(() -> harness.dispatcher().execute("warp teleport spawn", source))
                .isInstanceOf(CommandSyntaxException.class);
    }

    @Test
    void rejectsDuplicateCommandNameWithoutReplacingExistingDispatcherNode() throws Exception {
        var harness = TestEngine.harness();
        var source = new MockCommandSource("tester");
        source.addPermission("warp.use");
        var first = new IntegrationWarpCommand();
        var duplicate = new IntegrationWarpCommand();

        harness.engine().register(first);

        assertThatThrownBy(() -> harness.engine().register(duplicate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Command already registered: warp");

        harness.dispatcher().execute("warp broadcast still here", source);

        waitUntil(() -> first.events().size() == 1);

        assertThat(first.events()).containsExactly("broadcast:still here");
        assertThat(duplicate.events()).isEmpty();
    }

    @Test
    void recordsTelemetryForGeneratedAsyncHandlers() throws Exception {
        var adapter = new LocalBrigadierAdapter();
        var telemetry = new RecordingTelemetry();
        var engine =
                CommandEngine.builder().brigadier(adapter).telemetry(telemetry).build();
        var source = new MockCommandSource("tester");
        source.addPermission("warp.use");

        engine.register(new IntegrationWarpCommand());
        adapter.dispatcher().execute("warp teleport spawn", source);

        waitUntil(() -> telemetry.executions().contains("warp teleport:true"));

        assertThat(telemetry.executions()).contains("warp teleport:true");
    }

    @Test
    void usesConfiguredMessagesForInvalidSender() throws Exception {
        var adapter = new LocalBrigadierAdapter();
        var messages = new CommandMessages("internal-custom", "sender-custom", "syntax-custom", "permission-custom");
        var engine =
                CommandEngine.builder().brigadier(adapter).messages(messages).build();
        var source = new MockCommandSource("tester");
        var command = new IntegrationPlayerOnlyCommand();

        engine.register(command);
        adapter.dispatcher().execute("playeronly run", source);

        assertThat(source.messages()).containsExactly("sender-custom");
        assertThat(command.events()).isEmpty();
    }

    @Test
    void blocksCommandWhenRateLimiterRejectsExecution() throws Exception {
        var adapter = new LocalBrigadierAdapter();
        var attempts = new AtomicInteger();
        CommandRateLimiter limiter = (source, path) -> attempts.incrementAndGet() == 1;
        var messages = new CommandMessages(
                "internal-custom", "sender-custom", "syntax-custom", "permission-custom", "limited-custom");
        var engine = CommandEngine.builder()
                .brigadier(adapter)
                .messages(messages)
                .rateLimiter(limiter)
                .build();
        var source = new MockCommandSource("tester");
        source.addPermission("warp.use");
        var command = new IntegrationWarpCommand();

        engine.register(command);
        adapter.dispatcher().execute("warp broadcast first", source);
        adapter.dispatcher().execute("warp broadcast second", source);

        waitUntil(() -> command.events().size() == 1 && source.messages().size() == 1);

        assertThat(command.events()).containsExactly("broadcast:first");
        assertThat(source.messages()).containsExactly("limited-custom");
    }

    @Test
    void dispatchesManyGeneratedCommandsWithoutDroppingExecutions() throws Exception {
        var harness = TestEngine.harness();
        var source = new MockCommandSource("stress-tester");
        var command = new IntegrationStressCommand();

        harness.engine().register(command);
        for (int i = 0; i < 1_000; i++) {
            harness.dispatcher().execute("stress ping", source);
        }

        assertThat(command.executions()).isEqualTo(1_000);
    }

    private static void waitUntil(BooleanSupplier condition) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Timed out waiting for async command execution");
            }
            Thread.onSpinWait();
        }
    }

    private static final class InstantArgumentResolver implements ArgumentTypeResolver<Instant> {

        @Override
        public Class<Instant> type() {
            return Instant.class;
        }

        @Override
        public ArgumentType<?> argumentType() {
            return StringArgumentType.greedyString();
        }

        @Override
        public Instant resolve(CommandContext<?> context, String name) {
            return Instant.parse(StringArgumentType.getString(context, name));
        }
    }

    private static final class RecordingTelemetry implements CommandTelemetry {

        private final List<String> executions = new CopyOnWriteArrayList<>();
        private final List<String> failures = new CopyOnWriteArrayList<>();

        @Override
        public void recordExecution(CommandPath path, long nanos, boolean async) {
            executions.add(path + ":" + async);
        }

        @Override
        public void recordFailure(CommandPath path, String reason) {
            failures.add(path + ":" + reason);
        }

        @Override
        public void recordSuggestion(CommandPath path, long nanos, int suggestionCount) {}

        private List<String> executions() {
            return List.copyOf(executions);
        }
    }
}
