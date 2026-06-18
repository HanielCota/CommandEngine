package com.hanielfialho.platform.paper.argument;

import java.util.function.Function;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class PlayerArgumentResolver extends AbstractPaperArgumentResolver<Player> {

    public PlayerArgumentResolver() {
        this(Bukkit::getPlayerExact);
    }

    public PlayerArgumentResolver(@NotNull Function<String, Player> lookup) {
        super(Player.class, "player", lookup);
    }
}
