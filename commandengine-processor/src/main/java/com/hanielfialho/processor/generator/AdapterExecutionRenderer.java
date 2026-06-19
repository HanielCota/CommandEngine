package com.hanielfialho.processor.generator;

import com.hanielfialho.processor.model.CommandModel;
import com.hanielfialho.processor.model.ParameterModel;
import com.hanielfialho.processor.model.SubcommandModel;
import java.util.ArrayList;
import java.util.List;

final class AdapterExecutionRenderer {

    private final CommandModel model;

    AdapterExecutionRenderer(CommandModel model) {
        this.model = model;
    }

    void render(StringBuilder code) {
        for (int subIndex = 0; subIndex < model.getSubcommands().size(); subIndex++) {
            SubcommandModel subcommand = model.getSubcommands().get(subIndex);
            renderExecuteMethod(code, subcommand, subIndex);
        }
    }

    String argumentBuilderDeclaration(ParameterModel parameter, String variable, int subIndex) {
        String type = argumentBuilderType(parameter);
        return "RequiredArgumentBuilder<CommandSource, "
                + type
                + "> "
                + variable
                + " = RequiredArgumentBuilder.<CommandSource, "
                + type
                + ">argument(\""
                + AdapterRenderingSupport.escape(parameter.getName())
                + "\", "
                + argumentTypeExpression(parameter)
                + ")"
                + suggestionsExpression(parameter, subIndex);
    }

    private void renderExecuteMethod(StringBuilder code, SubcommandModel subcommand, int subIndex) {
        code.append("    private int execute").append(subIndex).append("(CommandContext<CommandSource> context) {\n");
        code.append("        CommandSource source = context.getSource();\n");

        List<ParameterModel> parameters = subcommand.getParameters();
        List<String> arguments = new ArrayList<>();
        for (int index = 0; index < parameters.size(); index++) {
            arguments.add("");
        }
        int senderIndex = 0;
        for (int parameterIndex = 0; parameterIndex < parameters.size(); parameterIndex++) {
            ParameterModel parameter = parameters.get(parameterIndex);
            if (parameter.getKind() == ParameterModel.Kind.SENDER) {
                arguments.set(parameterIndex, renderSenderParameter(code, parameter, senderIndex));
                senderIndex++;
            }
        }

        code.append("        if (!rateLimiter.tryAcquire(source, COMMAND_PATH_")
                .append(subIndex)
                .append(")) {\n");
        code.append("            scheduler.execute(() -> source.sendMessage(messages.rateLimited()));\n");
        code.append("            return 0;\n");
        code.append("        }\n");

        int argumentIndex = 0;
        int flagIndex = 0;
        for (int parameterIndex = 0; parameterIndex < parameters.size(); parameterIndex++) {
            ParameterModel parameter = parameters.get(parameterIndex);
            switch (parameter.getKind()) {
                case SENDER -> {}
                case ARGUMENT -> {
                    if (isCustomArgumentType(parameter)) {
                        arguments.set(parameterIndex, renderCustomArgumentExpression(parameter));
                    } else {
                        arguments.set(parameterIndex, renderArgumentParameter(code, parameter, argumentIndex));
                    }
                    argumentIndex++;
                }
                case FLAG -> {
                    arguments.set(parameterIndex, renderFlagParameter(code, parameter, flagIndex));
                    flagIndex++;
                }
            }
        }

        String invocation = "instance." + subcommand.getMethodName() + "(" + String.join(", ", arguments) + ")";
        if (subcommand.isAsync()) {
            renderAsyncInvocation(code, invocation, subIndex);
            return;
        }
        if (subcommand.returnsVoid()) {
            renderSyncVoidInvocation(code, invocation, subIndex);
            return;
        }
        renderIntInvocation(code, invocation, subIndex);
    }

    private String renderSenderParameter(StringBuilder code, ParameterModel parameter, int senderIndex) {
        String variable = "sender" + senderIndex;
        if ("com.hanielfialho.api.source.CommandSource".equals(parameter.getTypeName())) {
            code.append("        ")
                    .append(parameter.getTypeName())
                    .append(" ")
                    .append(variable)
                    .append(" = source;\n");
        }
        if (!"com.hanielfialho.api.source.CommandSource".equals(parameter.getTypeName())) {
            code.append("        if (!(source.getHandle() instanceof ")
                    .append(parameter.getTypeName())
                    .append(" ")
                    .append(variable)
                    .append(")) {\n");
            code.append("            scheduler.execute(() -> source.sendMessage(messages.invalidSender()));\n");
            code.append("            return 0;\n");
            code.append("        }\n");
        }
        return variable;
    }

    private String renderArgumentParameter(StringBuilder code, ParameterModel parameter, int argumentIndex) {
        String variable = "arg" + argumentIndex;
        code.append("        ")
                .append(parameter.getTypeName())
                .append(" ")
                .append(variable)
                .append(" = ")
                .append(argumentValueExpression(parameter))
                .append(";\n");
        renderStringLengthValidation(code, parameter, variable);
        return variable;
    }

    private String renderFlagParameter(StringBuilder code, ParameterModel parameter, int flagIndex) {
        String variable = "flag" + flagIndex;
        code.append("        boolean ")
                .append(variable)
                .append(" = hasFlag(context, \"")
                .append(AdapterRenderingSupport.escape(parameter.getName()))
                .append("\", '")
                .append(AdapterRenderingSupport.escapeChar(parameter.getShorthand()))
                .append("');\n");
        return variable;
    }

    private boolean isCustomArgumentType(ParameterModel parameter) {
        return switch (parameter.getTypeName()) {
            case "java.lang.String",
                    "java.lang.String[]",
                    "java.util.List<java.lang.String>",
                    "int",
                    "java.lang.Integer",
                    "long",
                    "java.lang.Long",
                    "float",
                    "java.lang.Float",
                    "double",
                    "java.lang.Double",
                    "boolean",
                    "java.lang.Boolean" -> false;
            default -> true;
        };
    }

    private String renderCustomArgumentExpression(ParameterModel parameter) {
        String argumentName = "\"" + AdapterRenderingSupport.escape(parameter.getName()) + "\"";
        String resolve = "resolveArgument(context, "
                + argumentName
                + ", "
                + AdapterRenderingSupport.classLiteral(parameter.getTypeName())
                + ")";
        if (!parameter.isOptional()) {
            return resolve;
        }
        return "hasArgument(context, " + argumentName + ") ? " + resolve + " : " + defaultValueExpression(parameter);
    }

    private void renderStringLengthValidation(StringBuilder code, ParameterModel parameter, String variable) {
        if (!"java.lang.String".equals(parameter.getTypeName())
                || (parameter.getMinLength() == 0 && parameter.getMaxLength() == Integer.MAX_VALUE)) {
            return;
        }
        code.append("        if (")
                .append(variable)
                .append(".length() < ")
                .append(parameter.getMinLength())
                .append(" || ")
                .append(variable)
                .append(".length() > ")
                .append(parameter.getMaxLength())
                .append(") {\n");
        code.append("            scheduler.execute(() -> source.sendMessage(messages.invalidSyntax()));\n");
        code.append("            return 0;\n");
        code.append("        }\n");
    }

    private void renderAsyncInvocation(StringBuilder code, String invocation, int subIndex) {
        code.append("        executor.executeAsync(source, COMMAND_PATH_")
                .append(subIndex)
                .append(", () -> ")
                .append(invocation)
                .append(").thenAccept(result -> handleResult(source, result, scheduler))\n");
        code.append(
                "            .exceptionally(throwable -> handleAsyncFailure(source, throwable, scheduler, messages));\n");
        code.append("        return 1;\n");
        code.append("    }\n\n");
    }

    private void renderSyncVoidInvocation(StringBuilder code, String invocation, int subIndex) {
        code.append("        return toBrigadierResult(source, executor.executeSync(source, COMMAND_PATH_")
                .append(subIndex)
                .append(", () -> ")
                .append(invocation)
                .append("), scheduler);\n");
        code.append("    }\n\n");
    }

    private void renderIntInvocation(StringBuilder code, String invocation, int subIndex) {
        code.append("        final int[] result = new int[1];\n");
        code.append("        CommandResult commandResult = executor.executeSync(source, COMMAND_PATH_")
                .append(subIndex)
                .append(", () -> result[0] = ")
                .append(invocation)
                .append(");\n");
        code.append("        if (commandResult instanceof CommandResult.Success) {\n");
        code.append(
                "            commandResult = result[0] < 0 ? CommandResult.failure(FailureReason.EXCEPTION, messages.internalError()) : CommandResult.success(result[0]);\n");
        code.append("        }\n");
        code.append("        return toBrigadierResult(source, commandResult, scheduler);\n");
        code.append("    }\n\n");
    }

    private String suggestionsExpression(ParameterModel parameter, int subIndex) {
        if (parameter.getSuggestionMethodName() == null) {
            return "";
        }
        return "\n            .suggests((context, builder) -> suggestFrom(builder, telemetry, COMMAND_PATH_"
                + subIndex
                + ", () -> instance."
                + parameter.getSuggestionMethodName()
                + "()))";
    }

    private String argumentBuilderType(ParameterModel parameter) {
        return switch (parameter.getTypeName()) {
            case "int", "java.lang.Integer" -> "Integer";
            case "long", "java.lang.Long" -> "Long";
            case "float", "java.lang.Float" -> "Float";
            case "double", "java.lang.Double" -> "Double";
            case "boolean", "java.lang.Boolean" -> "Boolean";
            case "java.lang.String" -> "String";
            case "java.lang.String[]", "java.util.List<java.lang.String>" -> "String";
            default -> "Object";
        };
    }

    private String argumentTypeExpression(ParameterModel parameter) {
        return switch (parameter.getTypeName()) {
            case "java.lang.String[]", "java.util.List<java.lang.String>" -> "StringArgumentType.greedyString()";
            case "java.lang.String" ->
                parameter.isGreedy() ? "StringArgumentType.greedyString()" : "StringArgumentType.string()";
            case "int", "java.lang.Integer" -> numericArgumentType("IntegerArgumentType.integer", parameter, "int");
            case "long", "java.lang.Long" -> numericArgumentType("LongArgumentType.longArg", parameter, "long");
            case "float", "java.lang.Float" -> numericArgumentType("FloatArgumentType.floatArg", parameter, "float");
            case "double", "java.lang.Double" ->
                numericArgumentType("DoubleArgumentType.doubleArg", parameter, "double");
            case "boolean", "java.lang.Boolean" -> "BoolArgumentType.bool()";
            default -> "argumentTypeFor(" + AdapterRenderingSupport.classLiteral(parameter.getTypeName()) + ")";
        };
    }

    private String numericArgumentType(String factory, ParameterModel parameter, String numericType) {
        Double min = parameter.getMin();
        Double max = parameter.getMax();
        if (min == null && max == null) {
            return factory + "()";
        }
        if (max == null) {
            return factory + "(" + numericLiteral(min, numericType) + ")";
        }
        return factory
                + "("
                + numericLiteral(min == null ? defaultMinimum(numericType) : min, numericType)
                + ", "
                + numericLiteral(max, numericType)
                + ")";
    }

    private double defaultMinimum(String numericType) {
        return switch (numericType) {
            case "int" -> Integer.MIN_VALUE;
            case "long" -> Long.MIN_VALUE;
            case "float" -> -Float.MAX_VALUE;
            default -> -Double.MAX_VALUE;
        };
    }

    private String numericLiteral(double value, String numericType) {
        return switch (numericType) {
            case "int" -> Integer.toString((int) value);
            case "long" -> Long.toString((long) value) + "L";
            case "float" -> Float.toString((float) value) + "F";
            default -> Double.toString(value);
        };
    }

    private String argumentValueExpression(ParameterModel parameter) {
        String expression = extractionExpression(parameter);
        if (!parameter.isOptional()) {
            return expression;
        }
        return "hasArgument(context, \""
                + AdapterRenderingSupport.escape(parameter.getName())
                + "\") ? "
                + expression
                + " : "
                + defaultValueExpression(parameter);
    }

    private String extractionExpression(ParameterModel parameter) {
        String argumentName = "\"" + AdapterRenderingSupport.escape(parameter.getName()) + "\"";
        return switch (parameter.getTypeName()) {
            case "java.lang.String" ->
                "stripFormattingCodes(StringArgumentType.getString(context, " + argumentName + "))";
            case "java.lang.String[]" ->
                "splitArguments(StringArgumentType.getString(context, " + argumentName + ")).toArray(String[]::new)";
            case "java.util.List<java.lang.String>" ->
                "splitArguments(StringArgumentType.getString(context, " + argumentName + "))";
            case "int", "java.lang.Integer" -> "IntegerArgumentType.getInteger(context, " + argumentName + ")";
            case "long", "java.lang.Long" -> "LongArgumentType.getLong(context, " + argumentName + ")";
            case "float", "java.lang.Float" -> "FloatArgumentType.getFloat(context, " + argumentName + ")";
            case "double", "java.lang.Double" -> "DoubleArgumentType.getDouble(context, " + argumentName + ")";
            case "boolean", "java.lang.Boolean" -> "BoolArgumentType.getBool(context, " + argumentName + ")";
            default ->
                "resolveArgument(context, "
                        + argumentName
                        + ", "
                        + AdapterRenderingSupport.classLiteral(parameter.getTypeName())
                        + ")";
        };
    }

    private String defaultValueExpression(ParameterModel parameter) {
        String value = parameter.getDefaultValue();
        return switch (parameter.getTypeName()) {
            case "java.lang.String" -> "\"" + AdapterRenderingSupport.escape(value == null ? "" : value) + "\"";
            case "java.lang.String[]" ->
                "splitArguments(\""
                        + AdapterRenderingSupport.escape(value == null ? "" : value)
                        + "\").toArray(String[]::new)";
            case "java.util.List<java.lang.String>" ->
                "splitArguments(\"" + AdapterRenderingSupport.escape(value == null ? "" : value) + "\")";
            case "int", "java.lang.Integer" -> Integer.toString(parseDefaultInt(value));
            case "long", "java.lang.Long" -> Long.toString(parseDefaultLong(value)) + "L";
            case "float", "java.lang.Float" -> Float.toString(parseDefaultFloat(value)) + "F";
            case "double", "java.lang.Double" -> Double.toString(parseDefaultDouble(value));
            case "boolean", "java.lang.Boolean" -> Boolean.toString(Boolean.parseBoolean(value));
            default ->
                value == null || value.isBlank()
                        ? "null"
                        : "resolveDefaultArgument(context, "
                                + AdapterRenderingSupport.classLiteral(parameter.getTypeName())
                                + ", \""
                                + AdapterRenderingSupport.escape(value)
                                + "\")";
        };
    }

    private int parseDefaultInt(String value) {
        return value == null || value.isBlank() ? 0 : Integer.parseInt(value);
    }

    private long parseDefaultLong(String value) {
        return value == null || value.isBlank() ? 0L : Long.parseLong(value);
    }

    private float parseDefaultFloat(String value) {
        return value == null || value.isBlank() ? 0F : Float.parseFloat(value);
    }

    private double parseDefaultDouble(String value) {
        return value == null || value.isBlank() ? 0D : Double.parseDouble(value);
    }
}
