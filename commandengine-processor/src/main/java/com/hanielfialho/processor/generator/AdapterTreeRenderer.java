package com.hanielfialho.processor.generator;

import com.hanielfialho.processor.model.CommandModel;
import com.hanielfialho.processor.model.ParameterModel;
import com.hanielfialho.processor.model.SubcommandModel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class AdapterTreeRenderer {

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
        code.append("        LiteralCommandNode<CommandSource> root = createTree(\"")
                .append(AdapterRenderingSupport.escape(model.getName()))
                .append("\").build();\n");
        code.append("        brigadier.register(root, metadata());\n");
        code.append("        registeredNames.add(\"")
                .append(AdapterRenderingSupport.escape(model.getName()))
                .append("\");\n");
        renderAliasRegistrations(code);
        code.append("    }\n\n");

        code.append("    @Override\n");
        code.append("    public void unregister(BrigadierAdapter brigadier) {\n");
        code.append("        Objects.requireNonNull(brigadier, \"brigadier\");\n");
        code.append("        for (String name : registeredNames) {\n");
        code.append("            brigadier.unregister(name);\n");
        code.append("        }\n");
        code.append("        registeredNames.clear();\n");
        code.append("    }\n\n");
    }

    private void renderAliasRegistrations(StringBuilder code) {
        String[] aliases = model.getAliases();
        for (int aliasIndex = 0; aliasIndex < aliases.length; aliasIndex++) {
            renderAliasRegistration(code, aliases[aliasIndex], aliasIndex);
        }
    }

    private void renderAliasRegistration(StringBuilder code, String alias, int aliasIndex) {
        code.append("        LiteralArgumentBuilder<CommandSource> aliasBuilder")
                .append(aliasIndex)
                .append(" = LiteralArgumentBuilder.<CommandSource>literal(\"")
                .append(AdapterRenderingSupport.escape(alias))
                .append("\")");
        if (!model.getPermission().isEmpty()) {
            code.append("\n            .requires(source -> source.hasPermission(\"")
                    .append(AdapterRenderingSupport.escape(model.getPermission()))
                    .append("\"))");
        }
        code.append(";\n");
        code.append("        if (root.getCommand() != null) {\n");
        code.append("            aliasBuilder").append(aliasIndex).append(".executes(root.getCommand());\n");
        code.append("        }\n");
        code.append("        LiteralCommandNode<CommandSource> alias")
                .append(aliasIndex)
                .append(" = aliasBuilder")
                .append(aliasIndex)
                .append(".build();\n");
        code.append("        for (CommandNode<CommandSource> child : root.getChildren()) {\n");
        code.append("            alias").append(aliasIndex).append(".addChild(child);\n");
        code.append("        }\n");
        code.append("        brigadier.register(alias").append(aliasIndex).append(", metadata());\n");
        code.append("        registeredNames.add(\"")
                .append(AdapterRenderingSupport.escape(alias))
                .append("\");\n");
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
        code.append("    }\n\n");
    }

    private void renderSubcommandTree(StringBuilder code, SubcommandModel subcommand, int subIndex) {
        String attachTo = "root";
        String[] pathParts = new String[0];
        boolean rootSubcommand = subcommand.getPath().isBlank();
        if (!rootSubcommand) {
            pathParts = subcommand.getPath().trim().split("\\s+");
            for (int pathIndex = 0; pathIndex < pathParts.length; pathIndex++) {
                String variable = "literal" + subIndex + "_" + pathIndex;
                code.append("        LiteralArgumentBuilder<CommandSource> ")
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
            attachTo = "literal" + subIndex + "_" + (pathParts.length - 1);
        }

        List<ParameterModel> commandArguments = subcommand.getParameters().stream()
                .filter(parameter -> parameter.getKind() == ParameterModel.Kind.ARGUMENT)
                .toList();
        List<ParameterModel> flags = subcommand.getParameters().stream()
                .filter(parameter -> parameter.getKind() == ParameterModel.Kind.FLAG)
                .toList();

        if (commandArguments.isEmpty()) {
            code.append("        ")
                    .append(attachTo)
                    .append(".executes(this::execute")
                    .append(subIndex)
                    .append(");\n");
            renderFlagNodes(code, attachTo, subIndex, flags, Set.of());
        }
        if (!commandArguments.isEmpty()) {
            renderArgumentNodes(code, attachTo, subIndex, commandArguments, flags);
        }

        if (!rootSubcommand) {
            for (int pathIndex = pathParts.length - 1; pathIndex > 0; pathIndex--) {
                code.append("        literal")
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
            code.append("        ")
                    .append(new AdapterExecutionRenderer(model).argumentBuilderDeclaration(parameter, variable))
                    .append(";\n");

            if (parameter.isOptional()) {
                String executableNode = previousArgument == null ? attachTo : previousArgument;
                code.append("        ")
                        .append(executableNode)
                        .append(".executes(this::execute")
                        .append(subIndex)
                        .append(");\n");
                renderFlagNodes(code, executableNode, subIndex, flags, Set.of());
            }

            if (argIndex == commandArguments.size() - 1) {
                code.append("        ")
                        .append(variable)
                        .append(".executes(this::execute")
                        .append(subIndex)
                        .append(");\n");
                renderFlagNodes(code, variable, subIndex, flags, Set.of());
            }
            previousArgument = variable;
        }
        for (int argIndex = argumentVariables.size() - 1; argIndex > 0; argIndex--) {
            code.append("        ")
                    .append(argumentVariables.get(argIndex - 1))
                    .append(".then(")
                    .append(argumentVariables.get(argIndex))
                    .append(");\n");
        }
        code.append("        ")
                .append(attachTo)
                .append(".then(")
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
            code.append("        LiteralArgumentBuilder<CommandSource> ")
                    .append(variable)
                    .append(" = LiteralArgumentBuilder.<CommandSource>literal(\"--")
                    .append(AdapterRenderingSupport.escape(flag.getName()))
                    .append("\").executes(this::execute")
                    .append(subIndex)
                    .append(");\n");
            Set<Integer> nextUsedFlags = new HashSet<>(usedFlags);
            nextUsedFlags.add(flagIndex);
            renderFlagNodes(code, variable, subIndex, flags, nextUsedFlags);
            code.append("        ")
                    .append(targetNode)
                    .append(".then(")
                    .append(variable)
                    .append(");\n");

            if (flag.getShorthand() != '\0') {
                String shorthandVariable = "flag" + subIndex + "_" + generatedNodeCounter++;
                code.append("        LiteralArgumentBuilder<CommandSource> ")
                        .append(shorthandVariable)
                        .append(" = LiteralArgumentBuilder.<CommandSource>literal(\"-")
                        .append(AdapterRenderingSupport.escape(Character.toString(flag.getShorthand())))
                        .append("\").executes(this::execute")
                        .append(subIndex)
                        .append(");\n");
                renderFlagNodes(code, shorthandVariable, subIndex, flags, nextUsedFlags);
                code.append("        ")
                        .append(targetNode)
                        .append(".then(")
                        .append(shorthandVariable)
                        .append(");\n");
            }
        }
    }
}
