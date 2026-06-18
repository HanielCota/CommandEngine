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
                .hasMessageContaining("asyncTimeout must be positive");
    }
}
