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
        Objects.requireNonNull(argumentResolvers, "argumentResolvers");
        return create(instance, executor);
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
        return create(instance, executor, argumentResolvers);
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
        Objects.requireNonNull(suggestionExecutor, "suggestionExecutor");
        return create(instance, executor, argumentResolvers, scheduler, messages, telemetry, rateLimiter);
    }

    default boolean supports(@NotNull Object instance) {
        return type().isInstance(Objects.requireNonNull(instance, "instance"));
    }

    default @NotNull CommandAdapter createAdapter(@NotNull Object instance, @NotNull CommandExecutor executor) {
        return create(
                type().cast(Objects.requireNonNull(instance, "instance")),
                Objects.requireNonNull(executor, "executor"));
    }

    default @NotNull CommandAdapter createAdapter(
            @NotNull Object instance,
            @NotNull CommandExecutor executor,
            @NotNull ArgumentResolverRegistry argumentResolvers) {
        return create(
                type().cast(Objects.requireNonNull(instance, "instance")),
                Objects.requireNonNull(executor, "executor"),
                Objects.requireNonNull(argumentResolvers, "argumentResolvers"));
    }

    default @NotNull CommandAdapter createAdapter(
            @NotNull Object instance,
            @NotNull CommandExecutor executor,
            @NotNull ArgumentResolverRegistry argumentResolvers,
            @NotNull CommandScheduler scheduler,
            @NotNull CommandMessages messages,
            @NotNull CommandTelemetry telemetry,
            @NotNull CommandRateLimiter rateLimiter) {
        return create(
                type().cast(Objects.requireNonNull(instance, "instance")),
                Objects.requireNonNull(executor, "executor"),
                Objects.requireNonNull(argumentResolvers, "argumentResolvers"),
                Objects.requireNonNull(scheduler, "scheduler"),
                Objects.requireNonNull(messages, "messages"),
                Objects.requireNonNull(telemetry, "telemetry"),
                Objects.requireNonNull(rateLimiter, "rateLimiter"));
    }

    default @NotNull CommandAdapter createAdapter(
            @NotNull Object instance,
            @NotNull CommandExecutor executor,
            @NotNull ArgumentResolverRegistry argumentResolvers,
            @NotNull CommandScheduler scheduler,
            @NotNull CommandMessages messages,
            @NotNull CommandTelemetry telemetry,
            @NotNull CommandRateLimiter rateLimiter,
            @NotNull SuggestionExecutor suggestionExecutor) {
        return create(
                type().cast(Objects.requireNonNull(instance, "instance")),
                Objects.requireNonNull(executor, "executor"),
                Objects.requireNonNull(argumentResolvers, "argumentResolvers"),
                Objects.requireNonNull(scheduler, "scheduler"),
                Objects.requireNonNull(messages, "messages"),
                Objects.requireNonNull(telemetry, "telemetry"),
                Objects.requireNonNull(rateLimiter, "rateLimiter"),
                Objects.requireNonNull(suggestionExecutor, "suggestionExecutor"));
    }
}
