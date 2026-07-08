/*
 * Copyright (c) 2026 Haniel Fialho
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
            var _ = release.await(5, TimeUnit.SECONDS); // result ignored: timeout is sufficient
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
