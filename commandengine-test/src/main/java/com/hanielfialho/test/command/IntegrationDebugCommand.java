package com.hanielfialho.test.command;

import com.hanielfialho.api.annotation.Command;
import com.hanielfialho.api.annotation.Flag;
import com.hanielfialho.api.annotation.Subcommand;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Command(name = "debug")
public final class IntegrationDebugCommand {

    private final List<String> events = new CopyOnWriteArrayList<>();

    @Subcommand("toggle")
    public void toggle(@Flag(value = "verbose", shorthand = 'v') boolean verbose) {
        events.add("verbose:" + verbose);
    }

    public List<String> events() {
        return List.copyOf(events);
    }
}
