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
import com.hanielfialho.api.telemetry.CommandTelemetry;
import com.hanielfialho.runtime.internal.argument.DefaultArgumentResolverRegistry;
import com.hanielfialho.runtime.internal.executor.TelemetryCommandExecutor;
import com.hanielfialho.runtime.internal.executor.VirtualThreadExecutor;
import com.hanielfialho.runtime.internal.rate.CaffeineCommandRateLimiter;
import com.hanielfialho.runtime.internal.registry.DefaultCommandRegistry;
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

    private static final Map<ClassLoader, List<CommandAdapterFactory>> FACTORIES_BY_CLASSLOADER =
            new ConcurrentHashMap<>();

    private final CommandRegistry registry;
    private final BrigadierAdapter brigadier;
    private final CommandExecutor executor;
    private final ArgumentResolverRegistry argumentResolvers;
    private final CommandScheduler scheduler;
    private final CommandMessages messages;
    private final CommandTelemetry telemetry;
    private final CommandRateLimiter rateLimiter;
    private final Object owner;
    private final Map<Object, CommandAdapter> adaptersByInstance;

    private CommandEngine(
            @NotNull CommandRegistry registry,
            @NotNull BrigadierAdapter brigadier,
            @NotNull CommandExecutor executor,
            @NotNull ArgumentResolverRegistry argumentResolvers,
            @NotNull CommandScheduler scheduler,
            @NotNull CommandMessages messages,
            @NotNull CommandTelemetry telemetry,
            @NotNull CommandRateLimiter rateLimiter,
            @NotNull Object owner) {
        this.registry = Preconditions.checkNotNull(registry, "registry");
        this.brigadier = Preconditions.checkNotNull(brigadier, "brigadier");
        this.executor = Preconditions.checkNotNull(executor, "executor");
        this.argumentResolvers = Preconditions.checkNotNull(argumentResolvers, "argumentResolvers");
        this.scheduler = Preconditions.checkNotNull(scheduler, "scheduler");
        this.messages = Preconditions.checkNotNull(messages, "messages");
        this.telemetry = Preconditions.checkNotNull(telemetry, "telemetry");
        this.rateLimiter = Preconditions.checkNotNull(rateLimiter, "rateLimiter");
        this.owner = Preconditions.checkNotNull(owner, "owner");
        this.adaptersByInstance = Collections.synchronizedMap(new IdentityHashMap<>());
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

    public static @NotNull CommandEngine create(@NotNull Platform platform) {
        Preconditions.checkNotNull(platform, "platform");
        return new CommandEngine(
                platform.registry(),
                platform.brigadier(),
                new TelemetryCommandExecutor(platform.executor(), platform.telemetry()),
                platform.argumentResolvers(),
                platform.scheduler(),
                platform.messages(),
                platform.telemetry(),
                platform.rateLimiter(),
                platform.owner());
    }

    public @NotNull CommandEngine register(@NotNull Object commandInstance) {
        Preconditions.checkNotNull(commandInstance, "commandInstance");
        CommandAdapter adapter = instantiateAdapter(commandInstance);
        synchronized (adaptersByInstance) {
            var existing = adaptersByInstance.get(commandInstance);
            if (existing != null) {
                unregister(existing);
            }
            try {
                register(adapter);
                adaptersByInstance.put(commandInstance, adapter);
            } catch (RuntimeException exception) {
                adaptersByInstance.remove(commandInstance);
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

    public @NotNull CommandEngine unregister(@NotNull CommandAdapter adapter) {
        Preconditions.checkNotNull(adapter, "adapter");
        adapter.unregister(brigadier);
        registry.unregister(adapter);
        synchronized (adaptersByInstance) {
            adaptersByInstance.values().removeIf(registered -> registered == adapter);
        }
        return this;
    }

    public void unregisterAll() {
        List<CommandAdapter> adapters;
        synchronized (adaptersByInstance) {
            adapters = List.copyOf(registry.getAdapters());
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
                        commandInstance, executor, argumentResolvers, scheduler, messages, telemetry, rateLimiter);
            }
        }

        throw new IllegalArgumentException("No generated adapter factory found for "
                + commandInstance.getClass().getName()
                + ". Ensure the class is annotated with @Command and compiled with CommandEngineProcessor.");
    }

    private static @NotNull List<CommandAdapterFactory> adapterFactories(@Nullable ClassLoader classLoader) {
        if (classLoader == null) {
            return ServiceLoader.load(CommandAdapterFactory.class).stream()
                    .map(ServiceLoader.Provider::get)
                    .toList();
        }
        return FACTORIES_BY_CLASSLOADER.computeIfAbsent(
                classLoader, loader -> ServiceLoader.load(CommandAdapterFactory.class, loader).stream()
                        .map(ServiceLoader.Provider::get)
                        .toList());
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
        if (executor instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception exception) {
                failure = addSuppressed(
                        failure, new IllegalStateException("Failed to close command executor", exception));
            }
        }
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
        Object owner();
    }

    public static final class Builder {

        private CommandRegistry registry = new DefaultCommandRegistry();
        private BrigadierAdapter brigadier;
        private CommandMessages messages = CommandMessages.defaults();
        private Duration asyncTimeout = Duration.ofSeconds(30);
        private CommandExecutor executor = new VirtualThreadExecutor(messages);
        private boolean customExecutor;
        private ArgumentResolverRegistry argumentResolvers = new DefaultArgumentResolverRegistry();
        private CommandScheduler scheduler = CommandScheduler.DIRECT;
        private CommandTelemetry telemetry = CommandTelemetry.NOOP;
        private CommandRateLimiter rateLimiter = CommandRateLimiter.NONE;
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
            if (!customExecutor && executor instanceof VirtualThreadExecutor) {
                this.executor = new VirtualThreadExecutor(messages, asyncTimeout);
            }
            return this;
        }

        public @NotNull Builder asyncTimeout(@NotNull Duration asyncTimeout) {
            Preconditions.checkNotNull(asyncTimeout, "asyncTimeout");
            if (asyncTimeout.isZero() || asyncTimeout.isNegative()) {
                throw new IllegalArgumentException("asyncTimeout must be positive");
            }
            this.asyncTimeout = asyncTimeout;
            if (!customExecutor && executor instanceof VirtualThreadExecutor) {
                this.executor = new VirtualThreadExecutor(messages, asyncTimeout);
            }
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
            return new CommandEngine(
                    registry,
                    brigadier,
                    new TelemetryCommandExecutor(executor, telemetry),
                    argumentResolvers,
                    scheduler,
                    messages,
                    telemetry,
                    rateLimiter,
                    owner);
        }
    }
}
