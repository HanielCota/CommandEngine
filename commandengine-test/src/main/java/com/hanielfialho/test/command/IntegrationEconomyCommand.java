package com.hanielfialho.test.command;

import com.hanielfialho.api.annotation.Arg;
import com.hanielfialho.api.annotation.Command;
import com.hanielfialho.api.annotation.Range;
import com.hanielfialho.api.annotation.Subcommand;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Command(name = "eco")
public final class IntegrationEconomyCommand {

    private final List<String> events = new CopyOnWriteArrayList<>();

    @Subcommand("pay")
    public void pay(@Arg("amount") @Range(min = 1, max = 100) int amount) {
        events.add("pay:" + amount);
    }

    public List<String> events() {
        return List.copyOf(events);
    }
}
