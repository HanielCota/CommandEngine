package com.hanielfialho.test.command;

import com.hanielfialho.api.annotation.Command;
import com.hanielfialho.api.annotation.Execute;
import com.hanielfialho.api.annotation.Subcommand;
import java.util.concurrent.atomic.AtomicInteger;

@Command(name = "stress")
public final class IntegrationStressCommand {

    private final AtomicInteger executions = new AtomicInteger();

    @Execute(async = false)
    @Subcommand("ping")
    public void ping() {
        executions.incrementAndGet();
    }

    public int executions() {
        return executions.get();
    }
}
