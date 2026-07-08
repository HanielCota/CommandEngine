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
        code.append("import com.mojang.brigadier.tree.ArgumentCommandNode;\n");
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
        code.append("import com.hanielfialho.api.suggestion.SuggestionExecutor;\n");
        code.append("import com.hanielfialho.api.telemetry.CommandTelemetry;\n");
        code.append("import java.util.List;\n");
        code.append("import java.util.Objects;\n");
        code.append("import java.util.concurrent.CompletableFuture;\n");
        code.append("import java.util.logging.Level;\n");
        code.append("import java.util.logging.Logger;\n");
        code.append("import java.util.function.Supplier;\n\n");
    }
}
