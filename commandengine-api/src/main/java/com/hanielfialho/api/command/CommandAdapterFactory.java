package com.hanielfialho.api.command;

import com.hanielfialho.api.argument.ArgumentResolverRegistry;
import com.hanielfialho.api.executor.CommandExecutor;
import com.hanielfialho.api.message.CommandMessages;
import com.hanielfialho.api.rate.CommandRateLimiter;
import com.hanielfialho.api.scheduler.CommandScheduler;
import com.hanielfialho.api.suggestion.SuggestionExecutor;
import com.hanielfialho.api.telemetry.CommandTelemetry;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Factory generated at compile time to create adapters without runtime reflection.
 */
public interface CommandAdapterFactory<T> {

    @NotNull
    Class<T> type();

    @NotNull
    CommandAdapter create(@NotNull T instance, @NotNull CommandExecutor executor);

    default @NotNull CommandAdapter create(
            @NotNull T instance,
            @NotNull CommandExecutor executor,
            @NotNull ArgumentResolverRegistry argumentResolvers) {
        throw new UnsupportedOperationException(
                "Full-argument create(...) must be implemented; this overload is not supported.");
    }

    default @NotNull CommandAdapter create(
            @NotNull T instance,
            @NotNull CommandExecutor executor,
            @NotNull ArgumentResolverRegistry argumentResolvers,
            @NotNull CommandScheduler scheduler,
            @NotNull CommandMessages messages,
            @NotNull CommandTelemetry telemetry,
            @NotNull CommandRateLimiter rateLimiter) {
        Objects.requireNonNull(scheduler, "scheduler");
        Objects.requireNonNull(messages, "messages");
        Objects.requireNonNull(telemetry, "telemetry");
        Objects.requireNonNull(rateLimiter, "rateLimiter");
        return create(
                instance,
                executor,
                argumentResolvers,
                scheduler,
                messages,
                telemetry,
                rateLimiter,
                SuggestionExecutor.DIRECT);
    }

    default @NotNull CommandAdapter create(
            @NotNull T instance,
            @NotNull CommandExecutor executor,
            @NotNull ArgumentResolverRegistry argumentResolvers,
            @NotNull CommandScheduler scheduler,
            @NotNull CommandMessages messages,
            @NotNull CommandTelemetry telemetry,
            @NotNull CommandRateLimiter rateLimiter,
            @NotNull SuggestionExecutor suggestionExecutor) {
        throw new UnsupportedOperationException(
                "Full-argument create(...) must be implemented; this overload is not supported.");
    }

    default boolean supports(@NotNull Object instance) {
        return type().isInstance(Objects.requireNonNull(instance, "instance"));
    }

    default @NotNull CommandAdapter createAdapter(@NotNull Object instance, @NotNull CommandExecutor executor) {
        return createAdapter(
                instance,
                executor,
                null,
                CommandScheduler.DIRECT,
                CommandMessages.defaults(),
                CommandTelemetry.NOOP,
                CommandRateLimiter.NONE,
                SuggestionExecutor.DIRECT);
    }

    default @NotNull CommandAdapter createAdapter(
            @NotNull Object instance,
            @NotNull CommandExecutor executor,
            @Nullable ArgumentResolverRegistry argumentResolvers) {
        return createAdapter(
                instance,
                executor,
                argumentResolvers,
                CommandScheduler.DIRECT,
                CommandMessages.defaults(),
                CommandTelemetry.NOOP,
                CommandRateLimiter.NONE,
                SuggestionExecutor.DIRECT);
    }

    default @NotNull CommandAdapter createAdapter(
            @NotNull Object instance,
            @NotNull CommandExecutor executor,
            @Nullable ArgumentResolverRegistry argumentResolvers,
            @NotNull CommandScheduler scheduler,
            @NotNull CommandMessages messages,
            @NotNull CommandTelemetry telemetry,
            @NotNull CommandRateLimiter rateLimiter) {
        return createAdapter(
                instance,
                executor,
                argumentResolvers,
                scheduler,
                messages,
                telemetry,
                rateLimiter,
                SuggestionExecutor.DIRECT);
    }

    default @NotNull CommandAdapter createAdapter(
            @NotNull Object instance,
            @NotNull CommandExecutor executor,
            @Nullable ArgumentResolverRegistry argumentResolvers,
            @NotNull CommandScheduler scheduler,
            @NotNull CommandMessages messages,
            @NotNull CommandTelemetry telemetry,
            @NotNull CommandRateLimiter rateLimiter,
            @NotNull SuggestionExecutor suggestionExecutor) {
        return create(
                type().cast(Objects.requireNonNull(instance, "instance")),
                Objects.requireNonNull(executor, "executor"),
                argumentResolvers,
                Objects.requireNonNull(scheduler, "scheduler"),
                Objects.requireNonNull(messages, "messages"),
                Objects.requireNonNull(telemetry, "telemetry"),
                Objects.requireNonNull(rateLimiter, "rateLimiter"),
                Objects.requireNonNull(suggestionExecutor, "suggestionExecutor"));
    }
}
