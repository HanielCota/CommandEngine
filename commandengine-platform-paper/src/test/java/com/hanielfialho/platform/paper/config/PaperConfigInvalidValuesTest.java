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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

final class PaperConfigInvalidValuesTest {

    @Test
    void negativeNumber() {
        var config = new YamlConfiguration();
        config.set("commandengine.async-timeout-millis", -1);

        assertThatThrownBy(() -> PaperCommandEngineConfigLoader.load(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be a positive number");
    }

    @Test
    void invalidEnum() {
        var config = new YamlConfiguration();
        config.set("commandengine.async-timeout-millis", "not-a-number");

        assertThatThrownBy(() -> PaperCommandEngineConfigLoader.load(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected a number");
    }

    @Test
    void invalidBoolean() {
        var config = new YamlConfiguration();
        config.set("commandengine.async-timeout-millis", true);

        assertThatThrownBy(() -> PaperCommandEngineConfigLoader.load(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected a number");
    }

    @Test
    void invalidList() {
        var config = new YamlConfiguration();
        config.set("commandengine.async-timeout-millis", java.util.List.of(1, 2, 3));

        assertThatThrownBy(() -> PaperCommandEngineConfigLoader.load(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected a number");
    }

    @Test
    void missingPath() {
        assertThatThrownBy(() ->
                        PaperCommandEngineConfigLoader.load((org.bukkit.configuration.file.FileConfiguration) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("config");
    }
}
