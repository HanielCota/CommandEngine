package com.hanielfialho.example.paper;

import com.hanielfialho.api.annotation.Arg;
import com.hanielfialho.api.annotation.Command;
import com.hanielfialho.api.annotation.Execute;
import com.hanielfialho.api.annotation.Greedy;
import com.hanielfialho.api.annotation.Sender;
import com.hanielfialho.api.annotation.Subcommand;
import com.hanielfialho.api.source.CommandSource;

@Command(
        name = "cexample",
        aliases = {"ce"},
        permission = "commandengine.example",
        description = "CommandEngine Paper example command")
public final class ExampleWarpCommand {

    @Execute(async = false)
    @Subcommand("ping")
    public void ping(@Sender CommandSource source) {
        source.sendMessage("Pong from CommandEngine.");
    }

    @Execute(async = false)
    @Subcommand("echo")
    public void echo(@Sender CommandSource source, @Arg("message") @Greedy String message) {
        source.sendMessage(message);
    }
}
