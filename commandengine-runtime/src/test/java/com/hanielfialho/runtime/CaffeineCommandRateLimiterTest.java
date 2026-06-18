package com.hanielfialho.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.hanielfialho.api.command.CommandPath;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.runtime.internal.rate.CaffeineCommandRateLimiter;
import java.time.Duration;
import org.junit.jupiter.api.Test;

final class CaffeineCommandRateLimiterTest {

    private static final CommandPath PATH = new CommandPath(new String[] {"guild", "create"});

    @Test
    void allowsUpToMaxExecutionsWithinWindow() {
        var limiter = new CaffeineCommandRateLimiter(Duration.ofMinutes(1), 3, 100);
        var source = new TestSource("player");

        assertThat(limiter.tryAcquire(source, PATH)).isTrue();
        assertThat(limiter.tryAcquire(source, PATH)).isTrue();
        assertThat(limiter.tryAcquire(source, PATH)).isTrue();
        assertThat(limiter.tryAcquire(source, PATH)).isFalse();
    }

    @Test
    void differentSendersHaveIndependentCounters() {
        var limiter = new CaffeineCommandRateLimiter(Duration.ofMinutes(1), 1, 100);
        var alice = new TestSource("alice");
        var bob = new TestSource("bob");

        assertThat(limiter.tryAcquire(alice, PATH)).isTrue();
        assertThat(limiter.tryAcquire(bob, PATH)).isTrue();
        assertThat(limiter.tryAcquire(alice, PATH)).isFalse();
        assertThat(limiter.tryAcquire(bob, PATH)).isFalse();
    }

    @Test
    void counterResetsAfterWindow() throws Exception {
        var limiter = new CaffeineCommandRateLimiter(Duration.ofMillis(50), 1, 100);
        var source = new TestSource("player");

        assertThat(limiter.tryAcquire(source, PATH)).isTrue();
        assertThat(limiter.tryAcquire(source, PATH)).isFalse();

        Thread.sleep(100);

        assertThat(limiter.tryAcquire(source, PATH)).isTrue();
    }

    @Test
    void rejectedAttemptsDoNotExtendWindow() throws Exception {
        var limiter = new CaffeineCommandRateLimiter(Duration.ofMillis(300), 1, 100);
        var source = new TestSource("player");

        assertThat(limiter.tryAcquire(source, PATH)).isTrue();

        for (int i = 0; i < 10; i++) {
            assertThat(limiter.tryAcquire(source, PATH)).isFalse();
            Thread.sleep(10);
        }

        assertThat(limiter.tryAcquire(source, PATH)).isFalse();

        Thread.sleep(250);
        assertThat(limiter.tryAcquire(source, PATH)).isTrue();
    }

    private static final class TestSource implements CommandSource {

        private final String name;

        private TestSource(String name) {
            this.name = name;
        }

        @Override
        public boolean hasPermission(String permission) {
            return true;
        }

        @Override
        public Object getHandle() {
            return this;
        }

        @Override
        public void sendMessage(String message) {}

        @Override
        public String getName() {
            return name;
        }
    }
}
