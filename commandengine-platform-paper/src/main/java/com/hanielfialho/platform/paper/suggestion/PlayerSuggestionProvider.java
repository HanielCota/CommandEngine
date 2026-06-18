package com.hanielfialho.platform.paper.suggestion;

import java.time.Duration;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class PlayerSuggestionProvider extends CachedSuggestionProvider {

    public PlayerSuggestionProvider() {
        super(Duration.ofMillis(500), 16, Bukkit::isPrimaryThread);
    }

    @Override
    protected @NotNull List<String> loadSuggestions() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }
}
