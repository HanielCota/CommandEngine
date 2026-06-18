package com.hanielfialho.platform.paper.argument;

import java.util.function.Function;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public final class WorldArgumentResolver extends AbstractPaperArgumentResolver<World> {

    public WorldArgumentResolver() {
        this(Bukkit::getWorld);
    }

    public WorldArgumentResolver(@NotNull Function<String, World> lookup) {
        super(World.class, "world", lookup);
    }
}
