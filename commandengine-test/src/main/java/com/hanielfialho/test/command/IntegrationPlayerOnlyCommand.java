package com.hanielfialho.test.command;

import com.hanielfialho.api.annotation.Command;
import com.hanielfialho.api.annotation.Sender;
import com.hanielfialho.api.annotation.Subcommand;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Command(name = "playeronly")
public final class IntegrationPlayerOnlyCommand {

    private final List<String> events = new CopyOnWriteArrayList<>();

    @Subcommand("run")
    public void run(@Sender TestPlayer player) {
        events.add(player.name());
    }

    public List<String> events() {
        return List.copyOf(events);
    }

    public record TestPlayer(String name) {}
}
