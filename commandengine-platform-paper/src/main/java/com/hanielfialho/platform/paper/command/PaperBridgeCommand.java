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

import com.hanielfialho.api.command.CommandMetadata;
import com.hanielfialho.api.message.CommandMessages;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.platform.paper.source.PaperCommandSource;
import com.hanielfialho.runtime.CommandEngineConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class PaperBridgeCommand extends Command {

    private final CommandDispatcher<CommandSource> dispatcher;
    private final Logger logger;
    private final CommandMessages messages;
    private final long tabCompleteTimeoutMillis;

    public PaperBridgeCommand(
            @NotNull CommandMetadata metadata,
            @NotNull CommandDispatcher<CommandSource> dispatcher,
            @NotNull Logger logger,
            @NotNull CommandMessages messages,
            @NotNull CommandEngineConfig config) {
        super(metadata.name());
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.tabCompleteTimeoutMillis = config.suggestionTimeout().toMillis();
        setDescription(metadata.description());
        setPermission(metadata.permission().isEmpty() ? null : metadata.permission());
        setAliases(metadata.aliases());
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        Objects.requireNonNull(args, "args");
        CommandSource source = new PaperCommandSource(sender);
        try {
            var _ = dispatcher.execute(commandLine(commandLabel, args), source);
            return true;
        } catch (CommandSyntaxException exception) {
            logger.log(Level.FINE, exception, () -> "Command syntax error for /" + commandLabel);
            sender.sendMessage(messages.invalidSyntax());
            return false;
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, exception, () -> "Command execution failed for /" + commandLabel);
            sender.sendMessage(messages.internalError());
            return true;
        }
    }

    @Override
    public @NotNull List<String> tabComplete(
            @NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        Objects.requireNonNull(args, "args");
        CommandSource source = new PaperCommandSource(sender);
        ParseResults<CommandSource> parse = dispatcher.parse(commandLine(alias, args), source);
        CompletableFuture<Suggestions> suggestionsFuture = dispatcher.getCompletionSuggestions(parse);
        try {
            Suggestions suggestions = suggestionsFuture.get(tabCompleteTimeoutMillis, TimeUnit.MILLISECONDS);
            return suggestions.getList().stream().map(Suggestion::getText).toList();
        } catch (CompletionException exception) {
            logger.log(Level.FINE, exception, () -> "Command completion failed for /" + alias);
            return List.of();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (TimeoutException exception) {
            if (!suggestionsFuture.cancel(true)) {
                logger.log(Level.FINE, "Failed to cancel timed-out command completion for /{0}", alias);
            }
            logger.log(Level.FINE, exception, () -> "Command completion timed out for /" + alias);
            return List.of();
        } catch (Exception exception) {
            logger.log(Level.FINE, exception, () -> "Command completion failed for /" + alias);
            return List.of();
        }
    }

    private String commandLine(String label, String[] args) {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(args, "args");
        if (args.length == 0) {
            return label;
        }
        return label + " " + String.join(" ", args);
    }
}
