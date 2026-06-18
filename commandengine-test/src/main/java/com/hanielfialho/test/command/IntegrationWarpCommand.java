package com.hanielfialho.test.command;

import com.hanielfialho.api.annotation.Arg;
import com.hanielfialho.api.annotation.Command;
import com.hanielfialho.api.annotation.Greedy;
import com.hanielfialho.api.annotation.Sender;
import com.hanielfialho.api.annotation.Subcommand;
import com.hanielfialho.api.annotation.SuggestionProvider;
import com.hanielfialho.api.annotation.Suggestions;
import com.hanielfialho.api.source.CommandSource;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Command(
        name = "warp",
        aliases = {"w"},
        permission = "warp.use")
public final class IntegrationWarpCommand {

    private final List<String> events = new CopyOnWriteArrayList<>();

    public void onCommand(CommandSource source, String values[]) {
        events.add("root:" + source.getName() + ":" + String.join(",", values));
    }

    @Subcommand("teleport")
    public void teleport(@Sender CommandSource source, @Arg("name") @Suggestions("warpNames") String warpName) {
        events.add("teleport:" + warpName + ":" + source.getName());
    }

    @Subcommand("broadcast")
    public void broadcast(@Arg("message") @Greedy String message) {
        events.add("broadcast:" + message);
    }

    @Subcommand("args")
    public void args(@Arg("values") String[] values) {
        events.add("args:" + String.join(",", values));
    }

    @Subcommand("list")
    public void list(@Arg("values") List<String> values) {
        events.add("list:" + String.join(",", values));
    }

    @SuggestionProvider("warpNames")
    public List<String> warpNames() {
        return List.of("spawn", "shop", "arena");
    }

    public List<String> events() {
        return List.copyOf(events);
    }
}
