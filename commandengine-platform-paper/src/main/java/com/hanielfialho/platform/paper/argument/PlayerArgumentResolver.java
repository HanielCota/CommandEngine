package com.hanielfialho.platform.paper.argument;

import java.util.function.Function;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class PlayerArgumentResolver extends AbstractPaperArgumentResolver<Player> {

    public PlayerArgumentResolver() {
        this(PlayerArgumentResolver::lookupPlayer);
    }

    public PlayerArgumentResolver(@NotNull Function<String, Player> lookup) {
        super(Player.class, "player", lookup);
    }

    private static Player lookupPlayer(String name) {
        Player exact = Bukkit.getPlayerExact(name);
        if (exact != null) {
            return exact;
        }
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
}
