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

import java.time.Duration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

final class PaperConfigDefaultsTest {

    @Test
    void emptyConfigUsesDefaults() {
        var config = new YamlConfiguration();
        var loaded = PaperCommandEngineConfigLoader.load(config);

        assertThat(loaded.asyncTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(loaded.suggestionTimeout()).isEqualTo(Duration.ofMillis(100));
        assertThat(loaded.rateLimitWindow()).isEqualTo(Duration.ofSeconds(2));
        assertThat(loaded.rateLimitMaxExecutions()).isEqualTo(5);
        assertThat(loaded.rateLimitMaximumSize()).isEqualTo(10_000);
    }

    @Test
    void missingFields() {
        var config = new YamlConfiguration();
        config.set("commandengine.async-timeout-millis", 15000);

        var loaded = PaperCommandEngineConfigLoader.load(config);

        assertThat(loaded.asyncTimeout()).isEqualTo(Duration.ofMillis(15000));
        assertThat(loaded.suggestionTimeout()).isEqualTo(Duration.ofMillis(100));
        assertThat(loaded.rateLimitMaxExecutions()).isEqualTo(5);
    }

    @Test
    void unknownSectionIgnored() {
        var config = new YamlConfiguration();
        config.set("commandengine.async-timeout-millis", 15000);
        config.set("some-random-section.unknown-key", "value");

        var loaded = PaperCommandEngineConfigLoader.load(config);

        assertThat(loaded.asyncTimeout()).isEqualTo(Duration.ofMillis(15000));
        assertThat(loaded.rateLimitMaxExecutions()).isEqualTo(5);
    }

    @Test
    void commentsPreserved() {
        var config = new YamlConfiguration();
        config.options().copyHeader(true);
        config.options().header("CommandEngine Configuration\n");

        var loaded = PaperCommandEngineConfigLoader.load(config);

        assertThat(loaded).isNotNull();
        assertThat(config.options().header()).contains("CommandEngine");
    }
}
