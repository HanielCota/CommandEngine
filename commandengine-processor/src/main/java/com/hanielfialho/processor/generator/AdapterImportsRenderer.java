package com.hanielfialho.processor.generator;

final class AdapterImportsRenderer {

    void render(StringBuilder code, String packageName) {
        if (!packageName.isEmpty()) {
            code.append("package ").append(packageName).append(";\n\n");
        }

        code.append("import com.mojang.brigadier.arguments.ArgumentType;\n");
        code.append("import com.mojang.brigadier.arguments.BoolArgumentType;\n");
        code.append("import com.mojang.brigadier.arguments.DoubleArgumentType;\n");
        code.append("import com.mojang.brigadier.arguments.FloatArgumentType;\n");
        code.append("import com.mojang.brigadier.arguments.IntegerArgumentType;\n");
        code.append("import com.mojang.brigadier.arguments.LongArgumentType;\n");
        code.append("import com.mojang.brigadier.arguments.StringArgumentType;\n");
        code.append("import com.mojang.brigadier.builder.LiteralArgumentBuilder;\n");
        code.append("import com.mojang.brigadier.builder.RequiredArgumentBuilder;\n");
        code.append("import com.mojang.brigadier.context.CommandContext;\n");
        code.append("import com.mojang.brigadier.suggestion.SuggestionsBuilder;\n");
        code.append("import com.mojang.brigadier.tree.CommandNode;\n");
        code.append("import com.mojang.brigadier.tree.LiteralCommandNode;\n");
        code.append("import com.hanielfialho.api.argument.ArgumentResolverRegistry;\n");
        code.append("import com.hanielfialho.api.argument.ArgumentTypeResolver;\n");
        code.append("import com.hanielfialho.api.command.CommandAdapter;\n");
        code.append("import com.hanielfialho.api.command.CommandMetadata;\n");
        code.append("import com.hanielfialho.api.command.CommandPath;\n");
        code.append("import com.hanielfialho.api.command.ParameterMetadata;\n");
        code.append("import com.hanielfialho.api.command.SubcommandMetadata;\n");
        code.append("import com.hanielfialho.api.executor.CommandExecutor;\n");
        code.append("import com.hanielfialho.api.message.CommandMessages;\n");
        code.append("import com.hanielfialho.api.rate.CommandRateLimiter;\n");
        code.append("import com.hanielfialho.api.registry.BrigadierAdapter;\n");
        code.append("import com.hanielfialho.api.result.CommandResult;\n");
        code.append("import com.hanielfialho.api.result.FailureReason;\n");
        code.append("import com.hanielfialho.api.scheduler.CommandScheduler;\n");
        code.append("import com.hanielfialho.api.source.CommandSource;\n");
        code.append("import com.hanielfialho.api.telemetry.CommandTelemetry;\n");
        code.append("import java.util.ArrayList;\n");
        code.append("import java.util.List;\n");
        code.append("import java.util.Objects;\n");
        code.append("import java.util.concurrent.CompletableFuture;\n\n");
    }
}
