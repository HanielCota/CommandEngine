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
                argumentResolvers != null ? argumentResolvers : ArgumentResolverRegistry.empty(),
                Objects.requireNonNull(scheduler, "scheduler"),
                Objects.requireNonNull(messages, "messages"),
                Objects.requireNonNull(telemetry, "telemetry"),
                Objects.requireNonNull(rateLimiter, "rateLimiter"),
                Objects.requireNonNull(suggestionExecutor, "suggestionExecutor"));
    }
}
