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
package com.hanielfialho.platform.paper.binding;

import static org.assertj.core.api.Assertions.assertThat;

import com.hanielfialho.api.source.CommandSource;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.lang.reflect.Proxy;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

final class PaperBrigadierSuggestionBridgeTest {

    @Test
    void synchronousSuggestions() {
        var dispatcher = new CommandDispatcher<CommandSource>();
        dispatcher.register(LiteralArgumentBuilder.<CommandSource>literal("root")
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("arg", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            builder.suggest("alpha");
                            builder.suggest("beta");
                            return builder.buildFuture();
                        })));
        var sender = senderProxy(true, "tester");

        var suggestions = dispatcher
                .getCompletionSuggestions(dispatcher.parse("root ", sender))
                .join();

        assertThat(suggestions.getList()).hasSize(2);
        assertThat(suggestions.getList().stream().map(s -> s.getText())).contains("alpha", "beta");
    }

    @Test
    void asynchronousSuggestions() {
        var dispatcher = new CommandDispatcher<CommandSource>();
        dispatcher.register(LiteralArgumentBuilder.<CommandSource>literal("root")
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("arg", StringArgumentType.word())
                        .suggests((context, builder) -> java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                            builder.suggest("async");
                            return builder.build();
                        }))));
        var sender = senderProxy(true, "tester");

        var suggestions = dispatcher
                .getCompletionSuggestions(dispatcher.parse("root ", sender))
                .join();

        assertThat(suggestions.getList()).hasSize(1);
        assertThat(suggestions.getList().get(0).getText()).isEqualTo("async");
    }

    @Test
    void suggestionsWithError() {
        var dispatcher = new CommandDispatcher<CommandSource>();
        dispatcher.register(LiteralArgumentBuilder.<CommandSource>literal("root")
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("arg", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            throw new RuntimeException("suggestion error");
                        })));
        var sender = senderProxy(true, "tester");

        try {
            var suggestions = dispatcher
                    .getCompletionSuggestions(dispatcher.parse("root ", sender))
                    .join();
            assertThat(suggestions.getList()).isEmpty();
        } catch (Exception expected) {
            assertThat(expected).isNotNull();
        }
    }

    @Test
    void suggestionsFilteredByPrefix() {
        var dispatcher = new CommandDispatcher<CommandSource>();
        dispatcher.register(LiteralArgumentBuilder.<CommandSource>literal("root")
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("arg", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            builder.suggest("apple");
                            builder.suggest("application");
                            builder.suggest("banana");
                            return builder.buildFuture();
                        })));
        var sender = senderProxy(true, "tester");

        var suggestions = dispatcher
                .getCompletionSuggestions(dispatcher.parse("root app", sender))
                .join();

        assertThat(suggestions.getList()).isNotEmpty();
        assertThat(suggestions.getList().stream().map(s -> s.getText())).anyMatch(t -> t.startsWith("app"));
    }

    @Test
    void emptySuggestions() {
        var dispatcher = new CommandDispatcher<CommandSource>();
        dispatcher.register(LiteralArgumentBuilder.<CommandSource>literal("root")
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("arg", StringArgumentType.word())
                        .suggests((context, builder) -> builder.buildFuture())));
        var sender = senderProxy(true, "tester");

        var suggestions = dispatcher
                .getCompletionSuggestions(dispatcher.parse("root ", sender))
                .join();

        assertThat(suggestions.getList()).isEmpty();
    }

    private static CommandSource senderProxy(boolean hasPerm, String name) {
        var sender = (CommandSender) Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] {CommandSender.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "hasPermission" -> hasPerm;
                    case "getName" -> name;
                    case "sendMessage" -> null;
                    default -> defaultValue(method.getReturnType());
                });
        return new com.hanielfialho.platform.paper.source.PaperCommandSource(sender);
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return null;
    }
}
