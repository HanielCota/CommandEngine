package com.hanielfialho.api.event;

import com.hanielfialho.api.command.CommandPath;
import com.hanielfialho.api.source.CommandSource;
import org.jetbrains.annotations.NotNull;

public interface CommandEvent {

    @NotNull
    CommandSource source();

    @NotNull
    CommandPath path();
}
