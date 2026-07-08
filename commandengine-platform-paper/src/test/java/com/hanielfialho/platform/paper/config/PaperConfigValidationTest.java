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
package com.hanielfialho.platform.paper.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

final class PaperConfigValidationTest {

    @Test
    void loadsConfiguredValues() {
        var config = new YamlConfiguration();
        config.set("commandengine.async-timeout-millis", 15000);
        config.set("commandengine.suggestion-timeout-millis", 5000);
        config.set("commandengine.rate-limit.window-millis", 2000);
        config.set("commandengine.rate-limit.max-executions", 10);
        config.set("commandengine.rate-limit.maximum-size", 500);
        config.set("commandengine.messages.internal-error", "custom-error");
        config.set("commandengine.messages.invalid-sender", "custom-sender");
        config.set("commandengine.messages.invalid-syntax", "custom-syntax");
        config.set("commandengine.messages.no-permission", "custom-permission");
        config.set("commandengine.messages.rate-limited", "custom-limited");

        var loaded = PaperCommandEngineConfigLoader.load(config);

        assertThat(loaded.asyncTimeout()).isEqualTo(Duration.ofMillis(15000));
        assertThat(loaded.messages().internalError()).isEqualTo("custom-error");
        assertThat(loaded.messages().rateLimited()).isEqualTo("custom-limited");
        assertThat(loaded.rateLimitWindow()).isEqualTo(Duration.ofMillis(2000));
        assertThat(loaded.rateLimitMaxExecutions()).isEqualTo(10);
    }

    @Test
    void usesDefaultsForMissingValues() {
        var config = new YamlConfiguration();
        var loaded = PaperCommandEngineConfigLoader.load(config);

        assertThat(loaded.asyncTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(loaded.messages().internalError()).isNotBlank();
        assertThat(loaded.rateLimitMaxExecutions()).isEqualTo(5);
    }

    @Test
    void rejectsInvalidTimeout() {
        var config = new YamlConfiguration();
        config.set("commandengine.async-timeout-millis", 0);
        assertThatThrownBy(() -> PaperCommandEngineConfigLoader.load(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be a positive number");
    }

    @Test
    void rejectsNegativeSuggestionTimeout() {
        var config = new YamlConfiguration();
        config.set("commandengine.suggestion-timeout-millis", -100);
        assertThatThrownBy(() -> PaperCommandEngineConfigLoader.load(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be a positive number");
    }

    @Test
    void rejectsStringForRateLimitWindow() {
        var config = new YamlConfiguration();
        config.set("commandengine.rate-limit.window-millis", "not-a-number");
        assertThatThrownBy(() -> PaperCommandEngineConfigLoader.load(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected a number");
    }

    @Test
    void rejectsStringForMessagePath() {
        var config = new YamlConfiguration();
        config.set("commandengine.messages.internal-error", 42);
        assertThatThrownBy(() -> PaperCommandEngineConfigLoader.load(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected a string");
    }

    @Test
    void rejectsZeroForMaxExecutions() {
        var config = new YamlConfiguration();
        config.set("commandengine.rate-limit.max-executions", 0);
        assertThatThrownBy(() -> PaperCommandEngineConfigLoader.load(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be >= 1");
    }

    @Test
    void rejectsNegativeForMaximumSize() {
        var config = new YamlConfiguration();
        config.set("commandengine.rate-limit.maximum-size", -1L);
        assertThatThrownBy(() -> PaperCommandEngineConfigLoader.load(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be >= 1");
    }
}
