package com.hanielfialho.runtime.internal.rate;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.Ticker;
import com.hanielfialho.api.command.CommandPath;
import com.hanielfialho.api.rate.CommandRateLimiter;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.runtime.util.Preconditions;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.NotNull;

public final class CaffeineCommandRateLimiter implements CommandRateLimiter, AutoCloseable {

    private static final String DEFAULT_BYPASS_PERMISSION = "commandengine.bypass.ratelimit";

    private final com.github.benmanes.caffeine.cache.Cache<Key, AtomicLong> executions;
    private final int maxExecutions;
    private final long windowNanos;
    private final String bypassPermission;

    public CaffeineCommandRateLimiter(@NotNull Duration window, int maxExecutions, long maximumSize) {
        this(window, maxExecutions, maximumSize, Ticker.systemTicker());
    }

    public CaffeineCommandRateLimiter(
            @NotNull Duration window, int maxExecutions, long maximumSize, @NotNull Ticker ticker) {
        this(window, maxExecutions, maximumSize, ticker, DEFAULT_BYPASS_PERMISSION);
    }

    public CaffeineCommandRateLimiter(
            @NotNull Duration window,
            int maxExecutions,
            long maximumSize,
            @NotNull Ticker ticker,
            @NotNull String bypassPermission) {
        Preconditions.checkNotNull(window, "window");
        Preconditions.checkNotNull(ticker, "ticker");
        Preconditions.checkNotNull(bypassPermission, "bypassPermission");
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
        this.windowNanos = window.toNanos();
        this.bypassPermission = bypassPermission;
        this.executions = Caffeine.newBuilder()
                .expireAfter(new WindowExpiry<>(windowNanos))
                .ticker(ticker)
                .maximumSize(maximumSize)
                .build();
    }

    @Override
    public void close() {
        executions.invalidateAll();
    }

    @Override
    public boolean tryAcquire(@NotNull CommandSource source, @NotNull CommandPath path) {
        if (source.hasPermission(bypassPermission)) {
            return true;
        }
        var key = new Key(Objects.requireNonNull(source, "source").getName(), Objects.requireNonNull(path, "path"));
        long current = executions.get(key, ignored -> new AtomicLong()).incrementAndGet();
        return current <= maxExecutions;
    }

    private record Key(@NotNull String senderName, @NotNull CommandPath path) {}

    private static final class WindowExpiry<K> implements Expiry<K, AtomicLong> {

        private final long windowNanos;

        private WindowExpiry(long windowNanos) {
            this.windowNanos = windowNanos;
        }

        @Override
        public long expireAfterCreate(@NotNull K key, @NotNull AtomicLong value, long currentTime) {
            return windowNanos;
        }

        @Override
        public long expireAfterUpdate(
                @NotNull K key, @NotNull AtomicLong value, long currentTime, long currentDuration) {
            return currentDuration;
        }

        @Override
        public long expireAfterRead(@NotNull K key, @NotNull AtomicLong value, long currentTime, long currentDuration) {
            return currentDuration;
        }
    }
}
