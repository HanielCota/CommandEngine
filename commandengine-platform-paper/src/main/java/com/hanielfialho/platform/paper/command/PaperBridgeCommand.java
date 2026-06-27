package com.hanielfialho.platform.paper.command;

import com.hanielfialho.api.command.CommandMetadata;
import com.hanielfialho.api.message.CommandMessages;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.platform.paper.source.PaperCommandSource;
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

    private static final long TAB_COMPLETE_TIMEOUT_MILLIS = 100L;

    private final CommandDispatcher<CommandSource> dispatcher;
    private final Logger logger;
    private final CommandMessages messages;

    public PaperBridgeCommand(
            @NotNull CommandMetadata metadata,
            @NotNull CommandDispatcher<CommandSource> dispatcher,
            @NotNull Logger logger,
            @NotNull CommandMessages messages) {
        super(metadata.name());
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.messages = Objects.requireNonNull(messages, "messages");
        setDescription(metadata.description());
        setPermission(metadata.permission().isEmpty() ? null : metadata.permission());
        setAliases(metadata.aliases());
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        Objects.requireNonNull(args, "args");
        CommandSource source = new PaperCommandSource(sender);
        try {
            dispatcher.execute(commandLine(commandLabel, args), source);
            return true;
        } catch (CommandSyntaxException exception) {
            logger.log(Level.FINE, exception, () -> "Command syntax error for /" + commandLabel);
            sender.sendMessage(exception.getMessage());
            return false;
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "Command execution failed for /" + commandLabel, exception);
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
            Suggestions suggestions = suggestionsFuture.get(TAB_COMPLETE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            return suggestions.getList().stream().map(Suggestion::getText).toList();
        } catch (CompletionException exception) {
            logger.log(Level.FINE, exception, () -> "Command completion failed for /" + alias);
            return List.of();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (TimeoutException exception) {
            suggestionsFuture.cancel(true);
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
