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

import com.hanielfialho.processor.model.CommandModel;

final class AdapterFactoryRenderer {

    private final CommandModel model;

    AdapterFactoryRenderer(CommandModel model) {
        this.model = model;
    }

    String render(String packageName, String adapterName, String factoryName) {
        StringBuilder code = new StringBuilder(2_048);
        if (!packageName.isEmpty()) {
            code.append("package ").append(packageName).append(";\n\n");
        }

        code.append("import com.hanielfialho.api.argument.ArgumentResolverRegistry;\n");
        code.append("import com.hanielfialho.api.command.CommandAdapter;\n");
        code.append("import com.hanielfialho.api.command.CommandAdapterFactory;\n");
        code.append("import com.hanielfialho.api.executor.CommandExecutor;\n");
        code.append("import com.hanielfialho.api.message.CommandMessages;\n");
        code.append("import com.hanielfialho.api.rate.CommandRateLimiter;\n");
        code.append("import com.hanielfialho.api.scheduler.CommandScheduler;\n");
        code.append("import com.hanielfialho.api.suggestion.SuggestionExecutor;\n");
        code.append("import com.hanielfialho.api.telemetry.CommandTelemetry;\n");
        code.append("import java.util.Objects;\n\n");

        String simpleClassName = model.getSimpleClassName().replace(".", "_");

        code.append("@SuppressWarnings({\"all\", \"NullableProblems\"})\n");
        code.append("public final class ")
                .append(factoryName)
                .append(" implements CommandAdapterFactory<")
                .append(simpleClassName)
                .append("> {\n\n");
        code.append("    @Override\n");
        code.append("    public Class<").append(simpleClassName).append("> type() {\n");
        code.append("        return ").append(model.getClassName()).append(".class;\n");
        code.append("    }\n\n");
        code.append("    @Override\n");
        code.append("    public CommandAdapter create(")
                .append(simpleClassName)
                .append(" instance, CommandExecutor executor) {\n");
        code.append("        return new ")
                .append(adapterName)
                .append(
                        "(Objects.requireNonNull(instance, \"instance\"), Objects.requireNonNull(executor, \"executor\"));\n");
        code.append("    }\n");
        code.append("\n");
        code.append("    @Override\n");
        code.append("    public CommandAdapter create(")
                .append(simpleClassName)
                .append(" instance, CommandExecutor executor, ArgumentResolverRegistry argumentResolvers) {\n");
        code.append("        return new ")
                .append(adapterName)
                .append(
                        "(Objects.requireNonNull(instance, \"instance\"), Objects.requireNonNull(executor, \"executor\"), ")
                .append("Objects.requireNonNull(argumentResolvers, \"argumentResolvers\"));\n");
        code.append("    }\n");
        code.append("\n");
        code.append("    @Override\n");
        code.append("    public CommandAdapter create(")
                .append(simpleClassName)
                .append(" instance, CommandExecutor executor, ArgumentResolverRegistry argumentResolvers, ")
                .append("CommandScheduler scheduler, CommandMessages messages, CommandTelemetry telemetry, ")
                .append("CommandRateLimiter rateLimiter) {\n");
        code.append("        return new ")
                .append(adapterName)
                .append(
                        "(Objects.requireNonNull(instance, \"instance\"), Objects.requireNonNull(executor, \"executor\"), ")
                .append("Objects.requireNonNull(argumentResolvers, \"argumentResolvers\"), ")
                .append("Objects.requireNonNull(scheduler, \"scheduler\"), ")
                .append("Objects.requireNonNull(messages, \"messages\"), ")
                .append("Objects.requireNonNull(telemetry, \"telemetry\"), ")
                .append("Objects.requireNonNull(rateLimiter, \"rateLimiter\"));\n");
        code.append("    }\n");
        code.append("\n");
        code.append("    @Override\n");
        code.append("    public CommandAdapter create(")
                .append(simpleClassName)
                .append(" instance, CommandExecutor executor, ArgumentResolverRegistry argumentResolvers, ")
                .append("CommandScheduler scheduler, CommandMessages messages, CommandTelemetry telemetry, ")
                .append("CommandRateLimiter rateLimiter, SuggestionExecutor suggestionExecutor) {\n");
        code.append("        return new ")
                .append(adapterName)
                .append(
                        "(Objects.requireNonNull(instance, \"instance\"), Objects.requireNonNull(executor, \"executor\"), ")
                .append("Objects.requireNonNull(argumentResolvers, \"argumentResolvers\"), ")
                .append("Objects.requireNonNull(scheduler, \"scheduler\"), ")
                .append("Objects.requireNonNull(messages, \"messages\"), ")
                .append("Objects.requireNonNull(telemetry, \"telemetry\"), ")
                .append("Objects.requireNonNull(rateLimiter, \"rateLimiter\"), ")
                .append("Objects.requireNonNull(suggestionExecutor, \"suggestionExecutor\"));\n");
        code.append("    }\n");
        code.append("}\n");
        return code.toString();
    }
}
