package com.hanielfialho.test.command;

import com.hanielfialho.api.annotation.Arg;
import com.hanielfialho.api.annotation.Command;
import com.hanielfialho.api.annotation.Subcommand;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Command(name = "admin")
public final class IntegrationSharedPrefixCommand {

    private final List<String> events = new CopyOnWriteArrayList<>();

    @Subcommand("player ban")
    public void ban(@Arg("name") String name) {
        events.add("ban:" + name);
    }

    @Subcommand("player kick")
    public void kick(@Arg("name") String name) {
        events.add("kick:" + name);
    }

    public List<String> events() {
        return List.copyOf(events);
    }
}
