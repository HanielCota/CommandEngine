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
package com.hanielfialho.platform.paper.argument;

import com.hanielfialho.api.argument.ArgumentTypeResolver;
import com.hanielfialho.api.source.CommandSource;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.Objects;
import java.util.function.DoubleSupplier;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

public final class LocationArgumentResolver implements ArgumentTypeResolver<Location> {

    private static final double MIN_COORDINATE = -30_000_000D;
    private static final double MAX_COORDINATE = 30_000_000D;

    @Override
    public @NotNull Class<Location> type() {
        return Location.class;
    }

    @Override
    public @NotNull ArgumentType<?> argumentType() {
        return StringArgumentType.greedyString();
    }

    @Override
    public @NotNull Location resolve(@NotNull CommandContext<?> context, @NotNull String name) {
        Objects.requireNonNull(context, "context");
        var raw = StringArgumentType.getString(context, Objects.requireNonNull(name, "name"));
        return resolveRaw(context, raw);
    }

    @Override
    public @NotNull Location resolveDefault(@NotNull CommandContext<?> context, @NotNull String input) {
        Objects.requireNonNull(context, "context");
        return resolveRaw(context, Objects.requireNonNull(input, "input"));
    }

    @Override
    public boolean supportsDefault() {
        return true;
    }

    private @NotNull Location resolveRaw(@NotNull CommandContext<?> context, @NotNull String raw) {
        Objects.requireNonNull(raw, "raw");
        var parts = raw.split(",");
        if (parts.length != 3 && parts.length != 4) {
            throw new IllegalArgumentException("Location must be x,y,z or world,x,y,z");
        }

        int offset = parts.length == 4 ? 1 : 0;
        var world = parts.length == 4 ? Bukkit.getWorld(parts[0].trim()) : worldFromSource(context.getSource());
        if (world == null) {
            throw new IllegalArgumentException("Location world could not be resolved");
        }

        var base = baseLocation(context.getSource(), world);
        return new Location(
                world,
                parseCoordinate(parts[offset], () -> base.getX()),
                parseCoordinate(parts[offset + 1], () -> base.getY()),
                parseCoordinate(parts[offset + 2], () -> base.getZ()));
    }

    private static @NotNull Location baseLocation(@NotNull Object source, @NotNull World fallback) {
        Objects.requireNonNull(fallback, "fallback");
        if (source instanceof CommandSource commandSource && commandSource.getHandle() instanceof Entity entity) {
            var location = entity.getLocation();
            if (location != null) {
                return location;
            }
        }
        return new Location(fallback, 0, 0, 0);
    }

    private static double parseCoordinate(@NotNull String value, @NotNull DoubleSupplier baseCoordinate) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(baseCoordinate, "baseCoordinate");
        var trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Location coordinate must not be empty");
        }
        boolean relative = trimmed.charAt(0) == '~';
        boolean local = trimmed.charAt(0) == '^';
        if (relative || local) {
            if (trimmed.length() == 1) {
                return baseCoordinate.getAsDouble();
            }
            trimmed = trimmed.substring(1);
        }
        double coordinate;
        try {
            coordinate = Double.parseDouble(trimmed);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Location coordinate must be a number", exception);
        }
        double result;
        if (local) {
            result = baseCoordinate.getAsDouble();
        } else if (relative) {
            result = baseCoordinate.getAsDouble() + coordinate;
        } else {
            result = coordinate;
        }
        if (!Double.isFinite(result) || result < MIN_COORDINATE || result > MAX_COORDINATE) {
            throw new IllegalArgumentException("Location coordinate is outside the supported world bounds");
        }
        return result;
    }

    private static World worldFromSource(Object source) {
        if (source instanceof CommandSource commandSource && commandSource.getHandle() instanceof Entity entity) {
            return entity.getWorld();
        }
        return null;
    }
}
