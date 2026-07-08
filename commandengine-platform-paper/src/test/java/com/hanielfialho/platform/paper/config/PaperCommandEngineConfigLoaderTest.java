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

final class PaperCommandEngineConfigLoaderTest {

    @Test
    void loadsConfiguredMessagesTimeoutAndRateLimit() {
        var yaml = new YamlConfiguration();
        yaml.set("commandengine.messages.internal-error", "internal");
        yaml.set("commandengine.messages.invalid-sender", "sender");
        yaml.set("commandengine.messages.invalid-syntax", "syntax");
        yaml.set("commandengine.messages.no-permission", "permission");
        yaml.set("commandengine.messages.rate-limited", "limited");
        yaml.set("commandengine.async-timeout-millis", 12_000L);
        yaml.set("commandengine.rate-limit.window-millis", 750L);
        yaml.set("commandengine.rate-limit.max-executions", 3);
        yaml.set("commandengine.rate-limit.maximum-size", 42L);

        var config = PaperCommandEngineConfigLoader.load(yaml);

        assertThat(config.messages().internalError()).isEqualTo("internal");
        assertThat(config.messages().invalidSender()).isEqualTo("sender");
        assertThat(config.messages().invalidSyntax()).isEqualTo("syntax");
        assertThat(config.messages().noPermission()).isEqualTo("permission");
        assertThat(config.messages().rateLimited()).isEqualTo("limited");
        assertThat(config.asyncTimeout()).isEqualTo(Duration.ofSeconds(12));
        assertThat(config.rateLimitWindow()).isEqualTo(Duration.ofMillis(750));
        assertThat(config.rateLimitMaxExecutions()).isEqualTo(3);
        assertThat(config.rateLimitMaximumSize()).isEqualTo(42);
    }

    @Test
    void usesDefaultsWhenValuesAreMissing() {
        var config = PaperCommandEngineConfigLoader.load(new YamlConfiguration());

        assertThat(config.asyncTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(config.rateLimitWindow()).isEqualTo(Duration.ofSeconds(2));
        assertThat(config.rateLimitMaxExecutions()).isEqualTo(5);
        assertThat(config.rateLimitMaximumSize()).isEqualTo(10_000);
    }

    @Test
    void rejectsInvalidDurationsFromConfiguration() {
        var yaml = new YamlConfiguration();
        yaml.set("commandengine.async-timeout-millis", 0L);

        assertThatThrownBy(() -> PaperCommandEngineConfigLoader.load(yaml))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("commandengine.async-timeout-millis must be a positive number");
    }
}
