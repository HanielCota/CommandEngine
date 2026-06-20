package com.hanielfialho.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hanielfialho.api.command.CommandAdapter;
import com.hanielfialho.api.command.CommandMetadata;
import com.hanielfialho.api.executor.CommandExecutor;
import com.hanielfialho.api.registry.BrigadierAdapter;
import com.hanielfialho.api.result.CommandResult;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.api.suggestion.SuggestionExecutor;
import com.hanielfialho.runtime.internal.executor.SyncExecutor;
import com.hanielfialho.runtime.internal.suggestion.VirtualThreadSuggestionExecutor;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

final class CommandEngineRegistrationTest {

    @Test
    void failedAdapterRegistrationIsRemovedFromRegistryAndDispatcher() {
        var brigadier = new TestBrigadierAdapter();
        try (var engine = CommandEngine.builder()
                .brigadier(brigadier)
                .executor(new SyncExecutor())
                .owner(this)
                .build()) {
            var adapter = new FailingAdapter("broken");

            assertThatThrownBy(() -> engine.register(adapter))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("boom");

            assertThat(engine.registry().getAdapters()).isEmpty();
            assertThat(brigadier.getDispatcher().getRoot().getChild("broken")).isNull();
            assertThat(adapter.unregistered).isTrue();
        }
    }

    @Test
    void unregisterAllKeepsRegistryAndDispatcherConsistentForAllAdapters() {
        var brigadier = new TestBrigadierAdapter();
        var registry = CommandEngine.defaultRegistry();
        try (var engine = CommandEngine.builder()
                .registry(registry)
                .brigadier(brigadier)
                .executor(new SyncExecutor())
                .owner("engine")
                .build()) {
            var adapter = new SimpleAdapter("external");

            registry.register("external-owner", adapter);
            adapter.register(brigadier);

            engine.unregisterAll();

            assertThat(registry.getAdapters()).isEmpty();
            assertThat(brigadier.getDispatcher().getRoot().getChild("external")).isNull();
            assertThat(adapter.unregistered).isTrue();
        }
    }

    @Test
    void unregisterAllProceedsDespitePartialFailures() {
        var brigadier = new TestBrigadierAdapter();
        var registry = CommandEngine.defaultRegistry();
        try (var engine = CommandEngine.builder()
                .registry(registry)
                .brigadier(brigadier)
                .executor(new SyncExecutor())
                .owner("engine")
                .build()) {
            var first = new FailingUnregisterAdapter("first");
            var second = new SimpleAdapter("second");

            registry.register("owner", first);
            first.register(brigadier);
            registry.register("owner", second);
            second.register(brigadier);

            assertThatThrownBy(engine::unregisterAll)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("unregister first");

            assertThat(registry.getAdapters()).isEmpty();
            assertThat(brigadier.getDispatcher().getRoot().getChild("first")).isNull();
            assertThat(brigadier.getDispatcher().getRoot().getChild("second")).isNull();
            assertThat(second.unregistered).isTrue();
        }
    }

    @Test
    void unregisterAdapterCleansRegistryEvenWhenAdapterUnregisterFails() {
        var brigadier = new TestBrigadierAdapter();
        var registry = CommandEngine.defaultRegistry();
        try (var engine = CommandEngine.builder()
                .registry(registry)
                .brigadier(brigadier)
                .executor(new SyncExecutor())
                .owner("engine")
                .build()) {
            var adapter = new FailingUnregisterAdapter("failing");

            engine.register(adapter);

            assertThatThrownBy(() -> engine.unregister(adapter))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("unregister failing");

            assertThat(registry.getAdapters()).isEmpty();
            assertThat(brigadier.getDispatcher().getRoot().getChild("failing")).isNull();
        }
    }

    @Test
    void closeUnregistersAllAndClosesExecutor() {
        var brigadier = new TestBrigadierAdapter();
        var registry = CommandEngine.defaultRegistry();
        var closableExecutor = new ClosableSyncExecutor();
        try (var engine = CommandEngine.builder()
                .registry(registry)
                .brigadier(brigadier)
                .executor(closableExecutor)
                .owner("engine")
                .build()) {
            var adapter = new SimpleAdapter("external");

            registry.register("owner", adapter);
            adapter.register(brigadier);
        }

        assertThat(registry.getAdapters()).isEmpty();
        assertThat(closableExecutor.closed).isTrue();
    }

    @Test
    void closeClosesSuggestionExecutor() {
        var suggestionExecutor = new ClosableSuggestionExecutor();
        try (var engine = CommandEngine.builder()
                .brigadier(new TestBrigadierAdapter())
                .executor(new SyncExecutor())
                .suggestionExecutor(suggestionExecutor)
                .build()) {
            assertThat(engine.registry().getAdapters()).isEmpty();
        }

        assertThat(suggestionExecutor.closed).isTrue();
    }

    @Test
    void virtualThreadSuggestionExecutorInterruptsTimedOutTasks() throws Exception {
        var started = new CountDownLatch(1);
        var finished = new CountDownLatch(1);
        var interrupted = new AtomicBoolean();
        try (var suggestionExecutor = new VirtualThreadSuggestionExecutor(Duration.ofMillis(10))) {
            CompletableFuture<String> result = suggestionExecutor.submit(() -> {
                started.countDown();
                try {
                    new CountDownLatch(1).await();
                } catch (InterruptedException exception) {
                    interrupted.set(true);
                    Thread.currentThread().interrupt();
                } finally {
                    finished.countDown();
                }
                return "done";
            });

            assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> result.get(1, TimeUnit.SECONDS)).hasCauseInstanceOf(TimeoutException.class);
            assertThat(finished.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(interrupted).isTrue();
        }
    }

    private static final class FailingAdapter implements CommandAdapter {

        private final CommandMetadata metadata;
        private boolean unregistered;

        private FailingAdapter(String name) {
            this.metadata = new CommandMetadata(name, List.of(), "", "", List.of());
        }

        @Override
        public void register(BrigadierAdapter brigadier) {
            brigadier.register(LiteralArgumentBuilder.<CommandSource>literal(metadata.name()), metadata);
            throw new IllegalStateException("boom");
        }

        @Override
        public void unregister(BrigadierAdapter brigadier) {
            unregistered = true;
            brigadier.unregister(metadata.name());
        }

        @Override
        public CommandMetadata metadata() {
            return metadata;
        }
    }

    private static final class SimpleAdapter implements CommandAdapter {

        private final CommandMetadata metadata;
        private boolean unregistered;

        private SimpleAdapter(String name) {
            this.metadata = new CommandMetadata(name, List.of(), "", "", List.of());
        }

        @Override
        public void register(BrigadierAdapter brigadier) {
            brigadier.register(LiteralArgumentBuilder.<CommandSource>literal(metadata.name()), metadata);
        }

        @Override
        public void unregister(BrigadierAdapter brigadier) {
            unregistered = true;
            brigadier.unregister(metadata.name());
        }

        @Override
        public CommandMetadata metadata() {
            return metadata;
        }
    }

    private static final class FailingUnregisterAdapter implements CommandAdapter {

        private final CommandMetadata metadata;

        private FailingUnregisterAdapter(String name) {
            this.metadata = new CommandMetadata(name, List.of(), "", "", List.of());
        }

        @Override
        public void register(BrigadierAdapter brigadier) {
            brigadier.register(LiteralArgumentBuilder.<CommandSource>literal(metadata.name()), metadata);
        }

        @Override
        public void unregister(BrigadierAdapter brigadier) {
            brigadier.unregister(metadata.name());
            throw new IllegalStateException("unregister " + metadata.name());
        }

        @Override
        public CommandMetadata metadata() {
            return metadata;
        }
    }

    private static final class ClosableSyncExecutor implements CommandExecutor, AutoCloseable {

        private boolean closed;

        @Override
        public CommandResult executeSync(CommandSource source, Runnable command) {
            command.run();
            return CommandResult.success();
        }

        @Override
        public CompletableFuture<CommandResult> executeAsync(CommandSource source, Runnable command) {
            return CompletableFuture.completedFuture(executeSync(source, command));
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static final class ClosableSuggestionExecutor implements SuggestionExecutor, AutoCloseable {

        private boolean closed;

        @Override
        public <T> CompletableFuture<T> submit(Supplier<T> task) {
            return CompletableFuture.completedFuture(task.get());
        }

        @Override
        public void close() {
            closed = true;
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
