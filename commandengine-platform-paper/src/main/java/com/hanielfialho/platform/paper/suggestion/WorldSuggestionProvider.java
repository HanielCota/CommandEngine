package com.hanielfialho.platform.paper.suggestion;

import java.time.Duration;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public final class WorldSuggestionProvider extends CachedSuggestionProvider {

    public WorldSuggestionProvider() {
        super(Duration.ofSeconds(2));
    }

    @Override
    protected @NotNull List<String> loadSuggestions() {
        return Bukkit.getWorlds().stream().map(World::getName).toList();
    }
}
