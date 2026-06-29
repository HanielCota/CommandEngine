package com.hanielfialho.runtime;

import com.hanielfialho.api.argument.ArgumentResolverRegistry;
import com.hanielfialho.api.argument.ArgumentTypeResolver;
import com.hanielfialho.api.command.CommandAdapter;
import com.hanielfialho.api.command.CommandAdapterFactory;
import com.hanielfialho.api.executor.CommandExecutor;
import com.hanielfialho.api.message.CommandMessages;
import com.hanielfialho.api.rate.CommandRateLimiter;
import com.hanielfialho.api.registry.BrigadierAdapter;
import com.hanielfialho.api.registry.CommandRegistry;
import com.hanielfialho.api.scheduler.CommandScheduler;
import com.hanielfialho.api.suggestion.SuggestionExecutor;
import com.hanielfialho.api.telemetry.CommandTelemetry;
import com.hanielfialho.runtime.internal.argument.DefaultArgumentResolverRegistry;
import com.hanielfialho.runtime.internal.executor.TelemetryCommandExecutor;
import com.hanielfialho.runtime.internal.executor.VirtualThreadExecutor;
import com.hanielfialho.runtime.internal.rate.CaffeineCommandRateLimiter;
import com.hanielfialho.runtime.internal.registry.DefaultCommandRegistry;
import com.hanielfialho.runtime.internal.suggestion.VirtualThreadSuggestionExecutor;
import com.hanielfialho.runtime.util.Preconditions;
import java.time.Duration;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Public facade used to register command instances through generated adapters.
 */
public final class CommandEngine implements AutoCloseable {

    private final CommandRegistry registry;
    private final BrigadierAdapter brigadier;
    private final CommandExecutor executor;
    private final ArgumentResolverRegistry argumentResolvers;
    private final CommandScheduler scheduler;
    private final CommandMessages messages;
    private final CommandTelemetry telemetry;
    private final CommandRateLimiter rateLimiter;
    private final SuggestionExecutor suggestionExecutor;
    private final Object owner;
    private final Map<Object, CommandAdapter> adaptersByInstance;
    private final Map<ClassLoader, List<CommandAdapterFactory>> adapterFactoriesByClassLoader;
    private final boolean customExecutor;
    private final boolean customSuggestionExecutor;

    @Nullable
    private volatile List<CommandAdapterFactory> bootstrapAdapterFactories;

    private CommandEngine(
            @NotNull CommandRegistry registry,
            @NotNull BrigadierAdapter brigadier,
            @NotNull CommandExecutor executor,
            @NotNull ArgumentResolverRegistry argumentResolvers,
            @NotNull CommandScheduler scheduler,
            @NotNull CommandMessages messages,
            @NotNull CommandTelemetry telemetry,
            @NotNull CommandRateLimiter rateLimiter,
            @NotNull SuggestionExecutor suggestionExecutor,
            @NotNull Object owner,
            boolean customExecutor,
            boolean customSuggestionExecutor) {
        this.registry = Preconditions.checkNotNull(registry, "registry");
        this.brigadier = Preconditions.checkNotNull(brigadier, "brigadier");
        this.executor = Preconditions.checkNotNull(executor, "executor");
        this.argumentResolvers = Preconditions.checkNotNull(argumentResolvers, "argumentResolvers");
        this.scheduler = Preconditions.checkNotNull(scheduler, "scheduler");
        this.messages = Preconditions.checkNotNull(messages, "messages");
        this.telemetry = Preconditions.checkNotNull(telemetry, "telemetry");
        this.rateLimiter = Preconditions.checkNotNull(rateLimiter, "rateLimiter");
        this.suggestionExecutor = Preconditions.checkNotNull(suggestionExecutor, "suggestionExecutor");
        this.owner = Preconditions.checkNotNull(owner, "owner");
        this.customExecutor = customExecutor;
        this.customSuggestionExecutor = customSuggestionExecutor;
        this.adaptersByInstance = Collections.synchronizedMap(new IdentityHashMap<>());
        this.adapterFactoriesByClassLoader = new ConcurrentHashMap<>();
    }

    public static @NotNull Builder builder() {
        return new Builder();
    }

    public static @NotNull CommandRegistry defaultRegistry() {
        return new DefaultCommandRegistry();
    }

    public static @NotNull CommandExecutor virtualThreadExecutor() {
        return new VirtualThreadExecutor();
    }

    public static @NotNull CommandExecutor virtualThreadExecutor(@NotNull CommandMessages messages) {
        return new VirtualThreadExecutor(messages);
    }

    public static @NotNull CommandExecutor virtualThreadExecutor(
            @NotNull CommandMessages messages, @NotNull Duration timeout) {
        return new VirtualThreadExecutor(messages, timeout);
    }

    public static @NotNull ArgumentResolverRegistry defaultArgumentResolverRegistry() {
        return new DefaultArgumentResolverRegistry();
    }

    public static @NotNull CommandRateLimiter configuredRateLimiter(@NotNull CommandEngineConfig config) {
        Preconditions.checkNotNull(config, "config");
        return new CaffeineCommandRateLimiter(
                config.rateLimitWindow(), config.rateLimitMaxExecutions(), config.rateLimitMaximumSize());
    }

    public static @NotNull SuggestionExecutor virtualThreadSuggestionExecutor() {
        return new VirtualThreadSuggestionExecutor();
    }

    public static @NotNull SuggestionExecutor virtualThreadSuggestionExecutor(@NotNull Duration timeout) {
        return new VirtualThreadSuggestionExecutor(timeout);
    }

    public static @NotNull CommandEngine create(@NotNull Platform platform) {
        Preconditions.checkNotNull(platform, "platform");
        CommandTelemetry telemetry = platform.telemetry();
        return new CommandEngine(
                platform.registry(),
                platform.brigadier(),
                instrumentExecutor(platform.executor(), telemetry),
                platform.argumentResolvers(),
                platform.scheduler(),
                platform.messages(),
                telemetry,
                platform.rateLimiter(),
                platform.suggestionExecutor(),
                platform.owner(),
                false,
                false);
    }

    @SuppressWarnings("java:S2201")
    public @NotNull CommandEngine register(@NotNull Object commandInstance) {
        Preconditions.checkNotNull(commandInstance, "commandInstance");
        CommandAdapter adapter = instantiateAdapter(commandInstance);
        synchronized (adaptersByInstance) {
            var existing = adaptersByInstance.get(commandInstance);
            if (existing != null) {
                adaptersByInstance.remove(commandInstance);
                unregister(existing);
            }
            try {
                register(adapter);
                adaptersByInstance.put(commandInstance, adapter);
            } catch (RuntimeException exception) {
                if (existing != null) {
                    try {
                        register(existing);
                        adaptersByInstance.put(commandInstance, existing);
                    } catch (RuntimeException restoreException) {
                        exception.addSuppressed(restoreException);
                    }
                }
                throw exception;
            }
        }
        return this;
    }

    public @NotNull CommandEngine register(@NotNull CommandAdapter adapter) {
        Preconditions.checkNotNull(adapter, "adapter");
        registry.register(owner, adapter);
        try {
            adapter.register(brigadier);
        } catch (RuntimeException exception) {
            try {
                adapter.unregister(brigadier);
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            registry.unregister(adapter);
            throw exception;
        }
        return this;
    }

    public @NotNull CommandEngine unregister(@NotNull Object commandInstance) {
        Preconditions.checkNotNull(commandInstance, "commandInstance");
        synchronized (adaptersByInstance) {
            CommandAdapter adapter = adaptersByInstance.remove(commandInstance);
            if (adapter == null) {
                throw new IllegalArgumentException("Command instance is not registered: "
                        + commandInstance.getClass().getName());
            }
            return unregister(adapter);
        }
    }

    @SuppressWarnings("java:S2201")
    public @NotNull CommandEngine unregister(@NotNull CommandAdapter adapter) {
        Preconditions.checkNotNull(adapter, "adapter");
        RuntimeException failure = null;
        try {
            adapter.unregister(brigadier);
        } catch (RuntimeException exception) {
            failure = exception;
        }
        try {
            registry.unregister(adapter);
        } catch (RuntimeException exception) {
            failure = addSuppressed(failure, exception);
        }
        synchronized (adaptersByInstance) {
            adaptersByInstance.values().removeIf(registered -> registered == adapter);
        }
        if (failure != null) {
            throw failure;
        }
        return this;
    }

    public void unregisterAll() {
        List<CommandAdapter> adapters;
        synchronized (adaptersByInstance) {
            adapters = List.copyOf(registry.getAdapters(owner));
        }

        RuntimeException failure = null;
        for (var adapter : adapters) {
            try {
                adapter.unregister(brigadier);
            } catch (RuntimeException exception) {
                failure = addSuppressed(failure, exception);
            }
            try {
                registry.unregister(adapter);
            } catch (RuntimeException exception) {
                failure = addSuppressed(failure, exception);
            }
        }

        synchronized (adaptersByInstance) {
            adaptersByInstance.clear();
        }

        if (failure != null) {
            throw failure;
        }
    }

    public @NotNull CommandRegistry registry() {
        return registry;
    }

    public @NotNull ArgumentResolverRegistry argumentResolvers() {
        return argumentResolvers;
    }

    private CommandAdapter instantiateAdapter(Object commandInstance) {
        Preconditions.checkNotNull(commandInstance, "commandInstance");
        var classLoader = commandInstance.getClass().getClassLoader();
        var factories = adapterFactories(classLoader);
        for (var factory : factories) {
            if (factory.supports(commandInstance)) {
                return factory.createAdapter(
                        commandInstance,
                        executor,
                        argumentResolvers,
                        scheduler,
                        messages,
                        telemetry,
                        rateLimiter,
                        suggestionExecutor);
            }
        }

        throw new IllegalArgumentException("No generated adapter factory found for "
                + commandInstance.getClass().getName()
                + ". Ensure: (1) the class is annotated with @Command, (2) the annotationProcessor dependency"
                + " is added for commandengine-processor, (3) module-info.java provides CommandAdapterFactory"
                + " if using JPMS.");
    }

    private @NotNull List<CommandAdapterFactory> adapterFactories(@Nullable ClassLoader classLoader) {
        if (classLoader == null) {
            return bootstrapAdapterFactories();
        }
        return adapterFactoriesByClassLoader.computeIfAbsent(classLoader, CommandEngine::loadAdapterFactories);
    }

    private @NotNull List<CommandAdapterFactory> bootstrapAdapterFactories() {
        List<CommandAdapterFactory> factories = bootstrapAdapterFactories;
        if (factories != null) {
            return factories;
        }
        factories = loadBootstrapAdapterFactories();
        bootstrapAdapterFactories = factories;
        return factories;
    }

    private static @NotNull List<CommandAdapterFactory> loadBootstrapAdapterFactories() {
        return ServiceLoader.load(CommandAdapterFactory.class).stream()
                .map(ServiceLoader.Provider::get)
                .toList();
    }

    private static @NotNull List<CommandAdapterFactory> loadAdapterFactories(@NotNull ClassLoader classLoader) {
        return ServiceLoader.load(CommandAdapterFactory.class, classLoader).stream()
                .map(ServiceLoader.Provider::get)
                .toList();
    }

    private static @NotNull CommandExecutor instrumentExecutor(
            @NotNull CommandExecutor executor, @NotNull CommandTelemetry telemetry) {
        Preconditions.checkNotNull(executor, "executor");
        Preconditions.checkNotNull(telemetry, "telemetry");
        return telemetry == CommandTelemetry.NOOP ? executor : new TelemetryCommandExecutor(executor, telemetry);
    }

    private static RuntimeException addSuppressed(RuntimeException failure, RuntimeException exception) {
        if (failure == null) {
            return exception;
        }
        failure.addSuppressed(exception);
        return failure;
    }

    @Override
    public void close() {
        RuntimeException failure = null;
        try {
            unregisterAll();
        } catch (RuntimeException exception) {
            failure = exception;
        }
        if (executor instanceof AutoCloseable closeable && !customExecutor) {
            try {
                closeable.close();
            } catch (Exception exception) {
                failure = addSuppressed(
                        failure, new IllegalStateException("Failed to close command executor", exception));
            }
        }
        if (suggestionExecutor instanceof AutoCloseable closeable && !customSuggestionExecutor) {
            try {
                closeable.close();
            } catch (Exception exception) {
                failure = addSuppressed(
                        failure, new IllegalStateException("Failed to close suggestion executor", exception));
            }
        }
        adapterFactoriesByClassLoader.clear();
        bootstrapAdapterFactories = null;
        if (failure != null) {
            throw failure;
        }
    }

    public interface Platform {

        @NotNull
        CommandRegistry registry();

        @NotNull
        BrigadierAdapter brigadier();

        @NotNull
        CommandExecutor executor();

        @NotNull
        default ArgumentResolverRegistry argumentResolvers() {
            return CommandEngine.defaultArgumentResolverRegistry();
        }

        @NotNull
        default CommandScheduler scheduler() {
            return CommandScheduler.DIRECT;
        }

        @NotNull
        default CommandMessages messages() {
            return CommandMessages.defaults();
        }

        @NotNull
        default CommandTelemetry telemetry() {
            return CommandTelemetry.NOOP;
        }

        @NotNull
        default CommandRateLimiter rateLimiter() {
            return CommandRateLimiter.NONE;
        }

        @NotNull
        default SuggestionExecutor suggestionExecutor() {
            return SuggestionExecutor.DIRECT;
        }

        @NotNull
        Object owner();
    }

    public static final class Builder {

        private CommandRegistry registry = new DefaultCommandRegistry();
        private BrigadierAdapter brigadier;
        private CommandMessages messages = CommandMessages.defaults();
        private Duration asyncTimeout = Duration.ofSeconds(30);
        private CommandExecutor executor;
        private boolean customExecutor;
        private ArgumentResolverRegistry argumentResolvers = new DefaultArgumentResolverRegistry();
        private CommandScheduler scheduler = CommandScheduler.DIRECT;
        private CommandTelemetry telemetry = CommandTelemetry.NOOP;
        private CommandRateLimiter rateLimiter = CommandRateLimiter.NONE;
        private SuggestionExecutor suggestionExecutor;
        private boolean customSuggestionExecutor;
        private Object owner = this;

        private Builder() {}

        public @NotNull Builder registry(@NotNull CommandRegistry registry) {
            this.registry = Preconditions.checkNotNull(registry, "registry");
            return this;
        }

        public @NotNull Builder brigadier(@NotNull BrigadierAdapter brigadier) {
            this.brigadier = Preconditions.checkNotNull(brigadier, "brigadier");
            return this;
        }

        public @NotNull Builder executor(@NotNull CommandExecutor executor) {
            this.executor = Preconditions.checkNotNull(executor, "executor");
            this.customExecutor = true;
            return this;
        }

        public @NotNull Builder scheduler(@NotNull CommandScheduler scheduler) {
            this.scheduler = Preconditions.checkNotNull(scheduler, "scheduler");
            return this;
        }

        public @NotNull Builder messages(@NotNull CommandMessages messages) {
            this.messages = Preconditions.checkNotNull(messages, "messages");
            return this;
        }

        public @NotNull Builder asyncTimeout(@NotNull Duration asyncTimeout) {
            Preconditions.checkNotNull(asyncTimeout, "asyncTimeout");
            if (asyncTimeout.isZero() || asyncTimeout.isNegative()) {
                throw new IllegalArgumentException("asyncTimeout must be positive");
            }
            this.asyncTimeout = asyncTimeout;
            return this;
        }

        public @NotNull Builder telemetry(@NotNull CommandTelemetry telemetry) {
            this.telemetry = Preconditions.checkNotNull(telemetry, "telemetry");
            return this;
        }

        public @NotNull Builder rateLimiter(@NotNull CommandRateLimiter rateLimiter) {
            this.rateLimiter = Preconditions.checkNotNull(rateLimiter, "rateLimiter");
            return this;
        }

        public @NotNull Builder suggestionExecutor(@NotNull SuggestionExecutor suggestionExecutor) {
            this.suggestionExecutor = Preconditions.checkNotNull(suggestionExecutor, "suggestionExecutor");
            this.customSuggestionExecutor = true;
            return this;
        }

        public @NotNull Builder config(@NotNull CommandEngineConfig config) {
            Preconditions.checkNotNull(config, "config");
            messages(config.messages());
            asyncTimeout(config.asyncTimeout());
            rateLimiter(CommandEngine.configuredRateLimiter(config));
            return this;
        }

        public @NotNull Builder argumentResolvers(@NotNull ArgumentResolverRegistry argumentResolvers) {
            this.argumentResolvers = Preconditions.checkNotNull(argumentResolvers, "argumentResolvers");
            return this;
        }

        public @NotNull Builder argumentResolver(@NotNull ArgumentTypeResolver<?> argumentResolver) {
            this.argumentResolvers.register(Preconditions.checkNotNull(argumentResolver, "argumentResolver"));
            return this;
        }

        public @NotNull Builder owner(@NotNull Object owner) {
            this.owner = Preconditions.checkNotNull(owner, "owner");
            return this;
        }

        public @NotNull CommandEngine build() {
            Preconditions.checkNotNull(brigadier, "brigadier");
            CommandExecutor selectedExecutor =
                    customExecutor ? executor : new VirtualThreadExecutor(messages, asyncTimeout);
            SuggestionExecutor selectedSuggestionExecutor =
                    customSuggestionExecutor ? suggestionExecutor : new VirtualThreadSuggestionExecutor(asyncTimeout);
            return new CommandEngine(
                    registry,
                    brigadier,
                    instrumentExecutor(selectedExecutor, telemetry),
                    argumentResolvers,
                    scheduler,
                    messages,
                    telemetry,
                    rateLimiter,
                    selectedSuggestionExecutor,
                    owner,
                    customExecutor,
                    customSuggestionExecutor);
        }
    }
}
