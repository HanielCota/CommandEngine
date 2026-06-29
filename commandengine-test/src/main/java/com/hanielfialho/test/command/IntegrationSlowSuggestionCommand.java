package com.hanielfialho.test.command;

import com.hanielfialho.api.annotation.Arg;
import com.hanielfialho.api.annotation.Command;
import com.hanielfialho.api.annotation.Subcommand;
import com.hanielfialho.api.annotation.SuggestionProvider;
import com.hanielfialho.api.annotation.Suggestions;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Command(name = "slow")
public final class IntegrationSlowSuggestionCommand {

    private final CountDownLatch started = new CountDownLatch(1);
    private final CountDownLatch release = new CountDownLatch(1);

    @Subcommand("run")
    public void run(@Arg("name") @Suggestions("names") String name) {
        // no-op: used only for testing suggestion execution
    }

    @SuggestionProvider(value = "names", async = true)
    public List<String> names() {
        started.countDown();
        try {
            release.await(5, TimeUnit.SECONDS); // result ignored: timeout is sufficient
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
        return List.of("spawn");
    }

    public boolean awaitStarted() throws InterruptedException {
        return started.await(1, TimeUnit.SECONDS);
    }

    public void release() {
        release.countDown();
    }
}
