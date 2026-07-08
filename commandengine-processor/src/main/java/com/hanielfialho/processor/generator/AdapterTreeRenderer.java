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
import com.hanielfialho.processor.model.ParameterModel;
import com.hanielfialho.processor.model.SubcommandModel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class AdapterTreeRenderer {

    private static final String CLOSE_BRACE = "    }\n\n";
    private static final String LITERAL_BUILDER_DECLARATION = "        LiteralArgumentBuilder<CommandSource> ";
    private static final String INDENT_8 = "        ";
    private static final String EXECUTES_METHOD = ".executes(this::execute";
    private static final String THEN_METHOD = ".then(";
    private static final String LITERAL_PREFIX = "literal";

    private final CommandModel model;
    private int generatedNodeCounter;

    AdapterTreeRenderer(CommandModel model) {
        this.model = model;
    }

    void render(StringBuilder code) {
        generatedNodeCounter = 0;
        renderRegister(code);
        renderCreateTree(code);
    }

    private void renderRegister(StringBuilder code) {
        code.append("    @Override\n");
        code.append("    public void register(BrigadierAdapter brigadier) {\n");
        code.append("        Objects.requireNonNull(brigadier, \"brigadier\");\n");
        code.append("        registeredNames.clear();\n");
        code.append("        registeredNames.add(\"")
                .append(AdapterRenderingSupport.escape(model.getName()))
                .append("\");\n");
        code.append("        LiteralCommandNode<CommandSource> root = createTree(\"")
                .append(AdapterRenderingSupport.escape(model.getName()))
                .append("\").build();\n");
        code.append("        brigadier.register(root, metadata());\n");
        renderAliasRegistrations(code);
        code.append(CLOSE_BRACE);

        code.append("    @Override\n");
        code.append("    public void unregister(BrigadierAdapter brigadier) {\n");
        code.append("        Objects.requireNonNull(brigadier, \"brigadier\");\n");
        code.append("        for (String name : registeredNames) {\n");
        code.append("            brigadier.unregister(name);\n");
        code.append("        }\n");
        code.append("        registeredNames.clear();\n");
        code.append(CLOSE_BRACE);
    }

    private void renderAliasRegistrations(StringBuilder code) {
        String[] aliases = model.getAliases();
        for (int aliasIndex = 0; aliasIndex < aliases.length; aliasIndex++) {
            renderAliasRegistration(code, aliases[aliasIndex], aliasIndex);
        }
    }

    private void renderAliasRegistration(StringBuilder code, String alias, int aliasIndex) {
        code.append("        LiteralCommandNode<CommandSource> alias")
                .append(aliasIndex)
                .append(" = createTree(\"")
                .append(AdapterRenderingSupport.escape(alias))
                .append("\").build();\n");
        code.append("        registeredNames.add(\"")
                .append(AdapterRenderingSupport.escape(alias))
                .append("\");\n");
        code.append("        brigadier.register(alias").append(aliasIndex).append(", metadata());\n");
    }

    private void renderCreateTree(StringBuilder code) {
        code.append("    private LiteralArgumentBuilder<CommandSource> createTree(String rootName) {\n");
        code.append(
                "        LiteralArgumentBuilder<CommandSource> root = LiteralArgumentBuilder.<CommandSource>literal(rootName)");
        if (!model.getPermission().isEmpty()) {
            code.append("\n            .requires(source -> source.hasPermission(\"")
                    .append(AdapterRenderingSupport.escape(model.getPermission()))
                    .append("\"))");
        }
        code.append(";\n\n");

        for (int subIndex = 0; subIndex < model.getSubcommands().size(); subIndex++) {
            SubcommandModel subcommand = model.getSubcommands().get(subIndex);
            renderSubcommandTree(code, subcommand, subIndex);
        }

        code.append("        return root;\n");
        code.append(CLOSE_BRACE);
    }

    private void renderSubcommandTree(StringBuilder code, SubcommandModel subcommand, int subIndex) {
        String attachTo = "root";
        String[] pathParts = new String[0];
        boolean rootSubcommand = subcommand.getPath().isBlank();
        if (!rootSubcommand) {
            pathParts = subcommand.getPath().trim().split("\\s+");
            for (int pathIndex = 0; pathIndex < pathParts.length; pathIndex++) {
                String variable = LITERAL_PREFIX + subIndex + "_" + pathIndex;
                code.append(LITERAL_BUILDER_DECLARATION)
                        .append(variable)
                        .append(" = LiteralArgumentBuilder.<CommandSource>literal(\"")
                        .append(AdapterRenderingSupport.escape(pathParts[pathIndex]))
                        .append("\")");
                if (pathIndex == pathParts.length - 1
                        && !subcommand.getPermission().isEmpty()) {
                    code.append("\n            .requires(source -> source.hasPermission(\"")
                            .append(AdapterRenderingSupport.escape(subcommand.getPermission()))
                            .append("\"))");
                }
                code.append(";\n");
            }
            attachTo = LITERAL_PREFIX + subIndex + "_" + (pathParts.length - 1);
        }

        List<ParameterModel> commandArguments = subcommand.getParameters().stream()
                .filter(parameter -> parameter.getKind() == ParameterModel.Kind.ARGUMENT)
                .toList();
        List<ParameterModel> flags = subcommand.getParameters().stream()
                .filter(parameter -> parameter.getKind() == ParameterModel.Kind.FLAG)
                .toList();

        if (commandArguments.isEmpty()) {
            code.append(INDENT_8)
                    .append(attachTo)
                    .append(EXECUTES_METHOD)
                    .append(subIndex)
                    .append(");\n");
            renderFlagNodes(code, attachTo, subIndex, flags, Set.of());
        }
        if (!commandArguments.isEmpty()) {
            renderArgumentNodes(code, attachTo, subIndex, commandArguments, flags);
        }

        if (!rootSubcommand) {
            for (int pathIndex = pathParts.length - 1; pathIndex > 0; pathIndex--) {
                code.append(INDENT_8)
                        .append(LITERAL_PREFIX)
                        .append(subIndex)
                        .append("_")
                        .append(pathIndex - 1)
                        .append(".then(literal")
                        .append(subIndex)
                        .append("_")
                        .append(pathIndex)
                        .append(");\n");
            }
            code.append("        root.then(literal").append(subIndex).append("_0);\n");
        }
        code.append("\n");
    }

    private void renderArgumentNodes(
            StringBuilder code,
            String attachTo,
            int subIndex,
            List<ParameterModel> commandArguments,
            List<ParameterModel> flags) {
        String previousArgument = null;
        List<String> argumentVariables = new ArrayList<>();
        for (int argIndex = 0; argIndex < commandArguments.size(); argIndex++) {
            ParameterModel parameter = commandArguments.get(argIndex);
            String variable = "argument" + subIndex + "_" + argIndex;
            argumentVariables.add(variable);
            code.append(INDENT_8)
                    .append(new AdapterExecutionRenderer(model)
                            .argumentBuilderDeclaration(parameter, variable, subIndex))
                    .append(";\n");

            if (parameter.isOptional()) {
                String executableNode = previousArgument == null ? attachTo : previousArgument;
                code.append(INDENT_8)
                        .append(executableNode)
                        .append(EXECUTES_METHOD)
                        .append(subIndex)
                        .append(");\n");
                renderFlagNodes(code, executableNode, subIndex, flags, Set.of());
            }

            if (argIndex == commandArguments.size() - 1) {
                code.append(INDENT_8)
                        .append(variable)
                        .append(EXECUTES_METHOD)
                        .append(subIndex)
                        .append(");\n");
                renderFlagNodes(code, variable, subIndex, flags, Set.of());
            }
            previousArgument = variable;
        }
        for (int argIndex = argumentVariables.size() - 1; argIndex > 0; argIndex--) {
            code.append(INDENT_8)
                    .append(argumentVariables.get(argIndex - 1))
                    .append(THEN_METHOD)
                    .append(argumentVariables.get(argIndex))
                    .append(");\n");
        }
        code.append(INDENT_8)
                .append(attachTo)
                .append(THEN_METHOD)
                .append(argumentVariables.getFirst())
                .append(");\n");
    }

    private void renderFlagNodes(
            StringBuilder code, String targetNode, int subIndex, List<ParameterModel> flags, Set<Integer> usedFlags) {
        for (int flagIndex = 0; flagIndex < flags.size(); flagIndex++) {
            if (usedFlags.contains(flagIndex)) {
                continue;
            }
            ParameterModel flag = flags.get(flagIndex);
            String variable = "flag" + subIndex + "_" + generatedNodeCounter++;
            code.append(LITERAL_BUILDER_DECLARATION)
                    .append(variable)
                    .append(" = LiteralArgumentBuilder.<CommandSource>literal(\"--")
                    .append(AdapterRenderingSupport.escape(flag.getName()))
                    .append("\").executes(this::execute")
                    .append(subIndex)
                    .append(");\n");
            Set<Integer> nextUsedFlags = new HashSet<>(usedFlags);
            nextUsedFlags.add(flagIndex);
            renderFlagNodes(code, variable, subIndex, flags, nextUsedFlags);
            code.append(INDENT_8)
                    .append(targetNode)
                    .append(THEN_METHOD)
                    .append(variable)
                    .append(");\n");

            if (flag.getShorthand() != '\0') {
                String shorthandVariable = "flag" + subIndex + "_" + generatedNodeCounter++;
                code.append(LITERAL_BUILDER_DECLARATION)
                        .append(shorthandVariable)
                        .append(" = LiteralArgumentBuilder.<CommandSource>literal(\"-")
                        .append(AdapterRenderingSupport.escape(Character.toString(flag.getShorthand())))
                        .append("\").executes(this::execute")
                        .append(subIndex)
                        .append(");\n");
                renderFlagNodes(code, shorthandVariable, subIndex, flags, nextUsedFlags);
                code.append(INDENT_8)
                        .append(targetNode)
                        .append(THEN_METHOD)
                        .append(shorthandVariable)
                        .append(");\n");
            }
        }
    }
}
