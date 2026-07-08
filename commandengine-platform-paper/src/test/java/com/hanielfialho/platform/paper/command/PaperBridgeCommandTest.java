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
package com.hanielfialho.platform.paper.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.hanielfialho.api.command.CommandMetadata;
import com.hanielfialho.api.message.CommandMessages;
import com.hanielfialho.api.source.CommandSource;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

final class PaperBridgeCommandTest {

    private static final CommandMessages MESSAGES =
            new CommandMessages("internal-custom", "sender-custom", "syntax-custom", "permission-custom");

    @Test
    void sendsBrigadierPermissionErrorMessageWhenRequiresRejects() {
        var dispatcher = new CommandDispatcher<CommandSource>();
        dispatcher.register(LiteralArgumentBuilder.<CommandSource>literal("root")
                .requires(source -> false)
                .executes(context -> 1));
        var sender = sender(false);
        var command = bridge(dispatcher, "root.use");

        boolean result = command.execute(sender.sender(), "root", new String[0]);

        assertThat(result).isFalse();
        assertThat(sender.messages()).isNotEmpty();
    }

    @Test
    void sendsBrigadierSyntaxMessageForInvalidInput() {
        var dispatcher = new CommandDispatcher<CommandSource>();
        dispatcher.register(LiteralArgumentBuilder.<CommandSource>literal("root")
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("name", StringArgumentType.word())
                        .executes(context -> 1)));
        var sender = sender(true);
        var command = bridge(dispatcher, "");

        boolean result = command.execute(sender.sender(), "root", new String[0]);

        assertThat(result).isFalse();
        assertThat(sender.messages()).isNotEmpty();
    }

    @Test
    void sendsInternalErrorMessageForRuntimeFailure() {
        var dispatcher = new CommandDispatcher<CommandSource>();
        dispatcher.register(
                LiteralArgumentBuilder.<CommandSource>literal("root").executes(context -> {
                    throw new IllegalStateException("secret-token");
                }));
        var sender = sender(true);
        var command = bridge(dispatcher, "");

        boolean result = command.execute(sender.sender(), "root", new String[0]);

        assertThat(result).isTrue();
        assertThat(sender.messages()).containsExactly("internal-custom");
    }

    @Test
    void treatsZeroBrigadierResultAsHandledCommand() {
        var dispatcher = new CommandDispatcher<CommandSource>();
        dispatcher.register(
                LiteralArgumentBuilder.<CommandSource>literal("root").executes(context -> 0));
        var sender = sender(true);
        var command = bridge(dispatcher, "");

        boolean result = command.execute(sender.sender(), "root", new String[0]);

        assertThat(result).isTrue();
        assertThat(sender.messages()).isEmpty();
    }

    @Test
    void returnsTabCompletionsFromDispatcher() {
        var dispatcher = new CommandDispatcher<CommandSource>();
        dispatcher.register(LiteralArgumentBuilder.<CommandSource>literal("root")
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("name", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            builder.suggest("spawn");
                            return builder.buildFuture();
                        })));
        var command = bridge(dispatcher, "");

        assertThat(command.tabComplete(sender(true).sender(), "root", new String[] {"s"}))
                .containsExactly("spawn");
    }

    @Test
    void returnsEmptyTabCompletionsWhenSuggestionFutureTimesOut() {
        var dispatcher = new CommandDispatcher<CommandSource>();
        var suggestions = new CompletableFuture<Suggestions>();
        dispatcher.register(LiteralArgumentBuilder.<CommandSource>literal("root")
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("name", StringArgumentType.word())
                        .suggests((context, builder) -> suggestions)));
        var command = bridge(dispatcher, "");

        assertThat(command.tabComplete(sender(true).sender(), "root", new String[] {"s"}))
                .isEmpty();
        suggestions.cancel(true);
    }

    private static PaperBridgeCommand bridge(CommandDispatcher<CommandSource> dispatcher, String permission) {
        return new PaperBridgeCommand(
                new CommandMetadata("root", List.of(), "", permission, List.of()),
                dispatcher,
                Logger.getLogger(PaperBridgeCommandTest.class.getName()),
                MESSAGES,
                com.hanielfialho.runtime.CommandEngineConfig.defaults());
    }

    private static SenderProbe sender(boolean permitted) {
        List<String> messages = new ArrayList<>();
        CommandSender sender = (CommandSender) Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] {CommandSender.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "hasPermission" -> permitted;
                    case "sendMessage" -> {
                        messages.add((String) args[0]);
                        yield null;
                    }
                    case "getName" -> "tester";
                    case "isPermissionSet" -> permitted;
                    case "isOp" -> permitted;
                    case "spigot" -> null;
                    default -> defaultValue(method.getReturnType());
                });
        return new SenderProbe(sender, messages);
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0F;
        }
        if (type == double.class) {
            return 0D;
        }
        return null;
    }

    private record SenderProbe(CommandSender sender, List<String> messages) {}
}
