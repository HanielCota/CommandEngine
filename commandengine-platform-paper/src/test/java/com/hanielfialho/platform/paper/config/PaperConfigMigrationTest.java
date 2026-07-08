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

final class PaperConfigMigrationTest {

    @Test
    void oldConfigVersion() {
        var config = new YamlConfiguration();
        config.set("commandengine.async-timeout-millis", 10000);

        var loaded = PaperCommandEngineConfigLoader.load(config);

        assertThat(loaded.asyncTimeout()).isEqualTo(Duration.ofMillis(10000));
    }

    @Test
    void migrationToNewVersion() {
        var config = new YamlConfiguration();
        config.set("commandengine.async-timeout-millis", 20000);
        config.set("commandengine.suggestion-timeout-millis", 3000);

        var loaded = PaperCommandEngineConfigLoader.load(config);

        assertThat(loaded.asyncTimeout()).isEqualTo(Duration.ofMillis(20000));
        assertThat(loaded.suggestionTimeout()).isEqualTo(Duration.ofMillis(3000));
    }

    @Test
    void backupCreated() {
        var config = new YamlConfiguration();
        config.set("commandengine.async-timeout-millis", 15000);

        var loaded = PaperCommandEngineConfigLoader.load(config);

        assertThat(loaded.asyncTimeout()).isEqualTo(Duration.ofMillis(15000));
        assertThat(loaded).isNotNull();
    }

    @Test
    void customValuePreserved() {
        var config = new YamlConfiguration();
        config.set("commandengine.messages.internal-error", "custom-internal");
        config.set("commandengine.messages.no-permission", "custom-perm");

        var loaded = PaperCommandEngineConfigLoader.load(config);

        assertThat(loaded.messages().internalError()).isEqualTo("custom-internal");
        assertThat(loaded.messages().noPermission()).isEqualTo("custom-perm");
    }
}
