package com.hanielfialho.example.paper;

import com.hanielfialho.platform.paper.PaperPlatform;
import com.hanielfialho.runtime.CommandEngine;
import org.bukkit.plugin.java.JavaPlugin;

public final class ExamplePaperPlugin extends JavaPlugin {

    private CommandEngine engine;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        engine = CommandEngine.create(PaperPlatform.create(this));
        engine.register(new ExampleWarpCommand());
    }

    @Override
    public void onDisable() {
        if (engine == null) {
            return;
        }

        engine.close();
        engine = null;
    }
}
