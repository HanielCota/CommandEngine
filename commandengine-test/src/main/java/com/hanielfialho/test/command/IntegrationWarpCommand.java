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
import com.hanielfialho.api.annotation.Execute;
import com.hanielfialho.api.annotation.Greedy;
import com.hanielfialho.api.annotation.Sender;
import com.hanielfialho.api.annotation.Subcommand;
import com.hanielfialho.api.annotation.SuggestionProvider;
import com.hanielfialho.api.annotation.Suggestions;
import com.hanielfialho.api.source.CommandSource;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Command(
        name = "warp",
        aliases = {"w"},
        permission = "warp.use")
public final class IntegrationWarpCommand {

    private final List<String> events = new CopyOnWriteArrayList<>();

    public void onCommand(CommandSource source, String[] values) {
        events.add("root:" + source.getName() + ":" + String.join(",", values));
    }

    @Subcommand("teleport")
    @Execute(async = true)
    public void teleport(@Sender CommandSource source, @Arg("name") @Suggestions("warpNames") String warpName) {
        events.add("teleport:" + warpName + ":" + source.getName());
    }

    @Subcommand("broadcast")
    public void broadcast(@Arg("message") @Greedy String message) {
        events.add("broadcast:" + message);
    }

    @Subcommand("args")
    public void args(@Arg("values") String[] values) {
        events.add("args:" + String.join(",", values));
    }

    @Subcommand("list")
    public void list(@Arg("values") List<String> values) {
        events.add("list:" + String.join(",", values));
    }

    @SuggestionProvider("warpNames")
    public List<String> warpNames() {
        return List.of("spawn", "shop", "arena");
    }

    public List<String> events() {
        return List.copyOf(events);
    }
}
