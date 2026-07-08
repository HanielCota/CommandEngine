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
package com.hanielfialho.test.examples;

import com.hanielfialho.api.annotation.Arg;
import com.hanielfialho.api.annotation.Command;
import com.hanielfialho.api.annotation.Greedy;
import com.hanielfialho.api.annotation.Range;
import com.hanielfialho.api.annotation.Sender;
import com.hanielfialho.api.annotation.Subcommand;
import com.hanielfialho.api.annotation.SuggestionProvider;
import com.hanielfialho.api.annotation.Suggestions;
import com.hanielfialho.api.source.CommandSource;
import java.util.List;
import java.util.Objects;

@Command(
        name = "warp",
        aliases = {"w"},
        permission = "warp.use",
        description = "Sistema de warps")
public final class WarpCommandExample {

    private final WarpService service;

    public WarpCommandExample(WarpService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @Subcommand("teleport")
    public void teleport(@Sender CommandSource source, @Arg("name") @Suggestions("warpNames") String warpName) {
        service.teleport(source, warpName);
    }

    @Subcommand(value = "create", permission = "warp.admin")
    public void create(
            @Sender CommandSource source,
            @Arg("name") String name,
            @Arg("cost") @Range(min = 0, max = 10_000) int cost) {
        service.create(source, name, cost);
    }

    @Subcommand(value = "broadcast", permission = "warp.admin")
    public void broadcast(@Sender CommandSource source, @Arg("message") @Greedy String message) {
        service.broadcast(source, message);
    }

    @SuggestionProvider("warpNames")
    public List<String> warpNames() {
        return service.warpNames();
    }

    public interface WarpService {

        void teleport(CommandSource source, String warpName);

        void create(CommandSource source, String name, int cost);

        void broadcast(CommandSource source, String message);

        List<String> warpNames();
    }
}
