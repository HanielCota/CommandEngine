package com.hanielfialho.platform.paper.argument;

import com.hanielfialho.api.argument.ArgumentTypeResolver;
import com.hanielfialho.api.source.CommandSource;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

public final class LocationArgumentResolver implements ArgumentTypeResolver<Location> {

    private static final double MIN_COORDINATE = -30_000_000D;
    private static final double MAX_COORDINATE = 30_000_000D;

    private static double parseCoordinate(String value) {
        double coordinate;
        try {
            coordinate = Double.parseDouble(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Location coordinate must be a number", exception);
        }
        if (!Double.isFinite(coordinate) || coordinate < MIN_COORDINATE || coordinate > MAX_COORDINATE) {
            throw new IllegalArgumentException("Location coordinate is outside the supported world bounds");
        }
        return coordinate;
    }

    private static World worldFromSource(Object source) {
        if (source instanceof CommandSource commandSource && commandSource.getHandle() instanceof Entity entity) {
            return entity.getWorld();
        }
        return null;
    }

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

    private @NotNull Location resolveRaw(CommandContext<?> context, String raw) {
        var parts = raw.split(",");
        if (parts.length != 3 && parts.length != 4) {
            throw new IllegalArgumentException("Location must be x,y,z or world,x,y,z");
        }

        int offset = parts.length == 4 ? 1 : 0;
        var world = parts.length == 4 ? Bukkit.getWorld(parts[0].trim()) : worldFromSource(context.getSource());
        if (world == null) {
            throw new IllegalArgumentException("Location world could not be resolved");
        }

        return new Location(
                world,
                parseCoordinate(parts[offset]),
                parseCoordinate(parts[offset + 1]),
                parseCoordinate(parts[offset + 2]));
    }
}
