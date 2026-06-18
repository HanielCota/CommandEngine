package com.hanielfialho.test.command;

import com.hanielfialho.api.annotation.Arg;
import com.hanielfialho.api.annotation.Command;
import com.hanielfialho.api.annotation.Subcommand;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Command(name = "time")
public final class IntegrationTimeCommand {

    private final List<String> events = new CopyOnWriteArrayList<>();

    @Subcommand("parse")
    public void parse(@Arg("instant") Instant instant) {
        events.add("instant:" + instant);
    }

    public List<String> events() {
        return List.copyOf(events);
    }
}
