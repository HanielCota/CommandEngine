package com.hanielfialho.runtime.internal.rate;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hanielfialho.api.command.CommandPath;
import com.hanielfialho.api.rate.CommandRateLimiter;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.runtime.util.Preconditions;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;

public final class CaffeineCommandRateLimiter implements CommandRateLimiter {

    private final Cache<Key, AtomicInteger> executions;
    private final int maxExecutions;

    public CaffeineCommandRateLimiter(@NotNull Duration window, int maxExecutions, long maximumSize) {
        Preconditions.checkNotNull(window, "window");
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive");
        }
        if (maxExecutions < 1) {
            throw new IllegalArgumentException("maxExecutions must be positive");
        }
        if (maximumSize < 1) {
            throw new IllegalArgumentException("maximumSize must be positive");
        }
        this.maxExecutions = maxExecutions;
        this.executions = Caffeine.newBuilder()
                .expireAfterWrite(window)
                .maximumSize(maximumSize)
                .build();
    }

    @Override
    public boolean tryAcquire(@NotNull CommandSource source, @NotNull CommandPath path) {
        var key = new Key(Objects.requireNonNull(source, "source").getName(), Objects.requireNonNull(path, "path"));
        int current = executions.get(key, ignored -> new AtomicInteger()).incrementAndGet();
        return current <= maxExecutions;
    }

    private record Key(@NotNull String senderName, @NotNull CommandPath path) {}
}
