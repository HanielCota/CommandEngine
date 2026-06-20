package com.hanielfialho.test.command;

import com.hanielfialho.api.annotation.Arg;
import com.hanielfialho.api.annotation.Command;
import com.hanielfialho.api.annotation.Optional;
import com.hanielfialho.api.annotation.Subcommand;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Command(name = "custom")
public final class IntegrationOptionalCustomCommand {

    private final List<String> events = new CopyOnWriteArrayList<>();

    @Subcommand("default")
    public void defaultTarget(@Arg("target") @Optional(defaultValue = "spawn") CustomTarget target) {
        events.add("target:" + target.value());
    }

    public List<String> events() {
        return List.copyOf(events);
    }

    public record CustomTarget(String value) {}
}
