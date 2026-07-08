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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hanielfialho.api.message.CommandMessages;
import java.time.Duration;
import org.junit.jupiter.api.Test;

final class CommandEngineConfigTest {

    @Test
    void defaultsCreatesValidConfig() {
        var config = CommandEngineConfig.defaults();
        assertThat(config.messages()).isNotNull();
        assertThat(config.asyncTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(config.suggestionTimeout()).isEqualTo(Duration.ofMillis(100));
        assertThat(config.rateLimitWindow()).isEqualTo(Duration.ofSeconds(2));
        assertThat(config.rateLimitMaxExecutions()).isEqualTo(5);
        assertThat(config.rateLimitMaximumSize()).isEqualTo(10_000);
    }

    @Test
    void throwsOnNullMessages() {
        assertThatThrownBy(() -> new CommandEngineConfig(
                        null, Duration.ofSeconds(1), Duration.ofMillis(1), Duration.ofMillis(1), 1, 1))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("messages");
    }

    @Test
    void throwsOnNullAsyncTimeout() {
        assertThatThrownBy(() -> new CommandEngineConfig(
                        CommandMessages.defaults(), null, Duration.ofMillis(1), Duration.ofMillis(1), 1, 1))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("asyncTimeout");
    }

    @Test
    void throwsOnZeroAsyncTimeout() {
        assertThatThrownBy(() -> new CommandEngineConfig(
                        CommandMessages.defaults(), Duration.ZERO, Duration.ofMillis(1), Duration.ofMillis(1), 1, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("asyncTimeout must be positive");
    }

    @Test
    void throwsOnNegativeAsyncTimeout() {
        assertThatThrownBy(() -> new CommandEngineConfig(
                        CommandMessages.defaults(),
                        Duration.ofSeconds(-1),
                        Duration.ofMillis(1),
                        Duration.ofMillis(1),
                        1,
                        1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("asyncTimeout must be positive");
    }

    @Test
    void throwsOnNullSuggestionTimeout() {
        assertThatThrownBy(() -> new CommandEngineConfig(
                        CommandMessages.defaults(), Duration.ofSeconds(1), null, Duration.ofMillis(1), 1, 1))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("suggestionTimeout");
    }

    @Test
    void throwsOnZeroSuggestionTimeout() {
        assertThatThrownBy(() -> new CommandEngineConfig(
                        CommandMessages.defaults(), Duration.ofSeconds(1), Duration.ZERO, Duration.ofMillis(1), 1, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("suggestionTimeout must be positive");
    }

    @Test
    void throwsOnNullRateLimitWindow() {
        assertThatThrownBy(() -> new CommandEngineConfig(
                        CommandMessages.defaults(), Duration.ofSeconds(1), Duration.ofMillis(1), null, 1, 1))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("rateLimitWindow");
    }

    @Test
    void throwsOnZeroRateLimitWindow() {
        assertThatThrownBy(() -> new CommandEngineConfig(
                        CommandMessages.defaults(), Duration.ofSeconds(1), Duration.ofMillis(1), Duration.ZERO, 1, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rateLimitWindow must be positive");
    }

    @Test
    void throwsOnZeroRateLimitMaxExecutions() {
        assertThatThrownBy(() -> new CommandEngineConfig(
                        CommandMessages.defaults(),
                        Duration.ofSeconds(1),
                        Duration.ofMillis(1),
                        Duration.ofMillis(1),
                        0,
                        1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rateLimitMaxExecutions must be positive");
    }

    @Test
    void throwsOnZeroRateLimitMaximumSize() {
        assertThatThrownBy(() -> new CommandEngineConfig(
                        CommandMessages.defaults(),
                        Duration.ofSeconds(1),
                        Duration.ofMillis(1),
                        Duration.ofMillis(1),
                        1,
                        0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rateLimitMaximumSize must be positive");
    }

    @Test
    void withMessagesReturnsCopyWithNewMessages() {
        var original = CommandEngineConfig.defaults();
        var custom = new CommandMessages("a", "b", "c", "d", "e");
        var copy = original.withMessages(custom);
        assertThat(copy.messages()).isEqualTo(custom);
        assertThat(copy.asyncTimeout()).isEqualTo(original.asyncTimeout());
    }

    @Test
    void withMessagesThrowsOnNull() {
        var config = CommandEngineConfig.defaults();
        assertThatThrownBy(() -> config.withMessages(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("messages");
    }

    @Test
    void withAsyncTimeoutReturnsCopyWithNewTimeout() {
        var original = CommandEngineConfig.defaults();
        var copy = original.withAsyncTimeout(Duration.ofSeconds(60));
        assertThat(copy.asyncTimeout()).isEqualTo(Duration.ofSeconds(60));
        assertThat(copy.messages()).isEqualTo(original.messages());
    }

    @Test
    void withAsyncTimeoutThrowsOnNull() {
        var config = CommandEngineConfig.defaults();
        assertThatThrownBy(() -> config.withAsyncTimeout(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("asyncTimeout");
    }

    @Test
    void withSuggestionTimeoutReturnsCopyWithNewTimeout() {
        var original = CommandEngineConfig.defaults();
        var copy = original.withSuggestionTimeout(Duration.ofSeconds(5));
        assertThat(copy.suggestionTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(copy.asyncTimeout()).isEqualTo(original.asyncTimeout());
    }

    @Test
    void withRateLimitReturnsCopyWithNewSettings() {
        var original = CommandEngineConfig.defaults();
        var copy = original.withRateLimit(Duration.ofMinutes(1), 10, 5000);
        assertThat(copy.rateLimitWindow()).isEqualTo(Duration.ofMinutes(1));
        assertThat(copy.rateLimitMaxExecutions()).isEqualTo(10);
        assertThat(copy.rateLimitMaximumSize()).isEqualTo(5000);
        assertThat(copy.asyncTimeout()).isEqualTo(original.asyncTimeout());
    }
}
