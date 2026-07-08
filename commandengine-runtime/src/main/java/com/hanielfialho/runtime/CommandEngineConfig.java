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

import com.hanielfialho.api.message.CommandMessages;
import java.time.Duration;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable runtime configuration for messages, async execution and built-in rate limiting.
 */
public record CommandEngineConfig(
        @NotNull CommandMessages messages,
        @NotNull Duration asyncTimeout,
        @NotNull Duration suggestionTimeout,
        @NotNull Duration rateLimitWindow,
        int rateLimitMaxExecutions,
        long rateLimitMaximumSize) {

    public CommandEngineConfig {
        Objects.requireNonNull(messages, "messages");
        Objects.requireNonNull(asyncTimeout, "asyncTimeout");
        Objects.requireNonNull(suggestionTimeout, "suggestionTimeout");
        Objects.requireNonNull(rateLimitWindow, "rateLimitWindow");
        if (asyncTimeout.isZero() || asyncTimeout.isNegative()) {
            throw new IllegalArgumentException("asyncTimeout must be positive");
        }
        if (suggestionTimeout.isZero() || suggestionTimeout.isNegative()) {
            throw new IllegalArgumentException("suggestionTimeout must be positive");
        }
        if (rateLimitWindow.isZero() || rateLimitWindow.isNegative()) {
            throw new IllegalArgumentException("rateLimitWindow must be positive");
        }
        if (rateLimitMaxExecutions < 1) {
            throw new IllegalArgumentException("rateLimitMaxExecutions must be positive");
        }
        if (rateLimitMaximumSize < 1) {
            throw new IllegalArgumentException("rateLimitMaximumSize must be positive");
        }
    }

    /**
     * Creates the default production-oriented runtime configuration.
     *
     * @return default runtime configuration
     */
    public static @NotNull CommandEngineConfig defaults() {
        return new CommandEngineConfig(
                CommandMessages.defaults(),
                Duration.ofSeconds(30),
                Duration.ofMillis(100),
                Duration.ofSeconds(2),
                5,
                10_000);
    }

    /**
     * Returns a copy of this config with the given messages.
     */
    public @NotNull CommandEngineConfig withMessages(@NotNull CommandMessages messages) {
        Objects.requireNonNull(messages, "messages");
        return new CommandEngineConfig(
                messages,
                asyncTimeout,
                suggestionTimeout,
                rateLimitWindow,
                rateLimitMaxExecutions,
                rateLimitMaximumSize);
    }

    /**
     * Returns a copy of this config with the given async execution timeout.
     */
    public @NotNull CommandEngineConfig withAsyncTimeout(@NotNull Duration asyncTimeout) {
        Objects.requireNonNull(asyncTimeout, "asyncTimeout");
        return new CommandEngineConfig(
                messages,
                asyncTimeout,
                suggestionTimeout,
                rateLimitWindow,
                rateLimitMaxExecutions,
                rateLimitMaximumSize);
    }

    /**
     * Returns a copy of this config with the given suggestion timeout.
     */
    public @NotNull CommandEngineConfig withSuggestionTimeout(@NotNull Duration suggestionTimeout) {
        Objects.requireNonNull(suggestionTimeout, "suggestionTimeout");
        return new CommandEngineConfig(
                messages,
                asyncTimeout,
                suggestionTimeout,
                rateLimitWindow,
                rateLimitMaxExecutions,
                rateLimitMaximumSize);
    }

    /**
     * Returns a copy of this config with the given rate-limit settings.
     */
    public @NotNull CommandEngineConfig withRateLimit(
            @NotNull Duration rateLimitWindow, int rateLimitMaxExecutions, long rateLimitMaximumSize) {
        Objects.requireNonNull(rateLimitWindow, "rateLimitWindow");
        return new CommandEngineConfig(
                messages,
                asyncTimeout,
                suggestionTimeout,
                rateLimitWindow,
                rateLimitMaxExecutions,
                rateLimitMaximumSize);
    }
}
