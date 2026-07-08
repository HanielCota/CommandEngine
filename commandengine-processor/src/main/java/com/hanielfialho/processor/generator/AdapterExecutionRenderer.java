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
import java.util.List;

final class AdapterExecutionRenderer {

    private static final String RETURN_ZERO = "            return 0;\n";
    private static final String CLOSE_BRACE = "        }\n";
    private static final String JAVA_STRING = "java.lang.String";
    private static final String JAVA_STRING_ARRAY = "java.lang.String[]";
    private static final String JAVA_STRING_LIST = "java.util.List<java.lang.String>";
    private static final String JAVA_INT_OBJ = "java.lang.Integer";
    private static final String JAVA_LONG_OBJ = "java.lang.Long";
    private static final String TYPE_FLOAT = "float";
    private static final String JAVA_FLOAT_OBJ = "java.lang.Float";
    private static final String TYPE_DOUBLE = "double";
    private static final String JAVA_DOUBLE_OBJ = "java.lang.Double";
    private static final String TYPE_BOOLEAN = "boolean";
    private static final String JAVA_BOOLEAN_OBJ = "java.lang.Boolean";
    private static final String CLOSE_BRACE_METHOD = "    }\n\n";

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
                arguments.set(parameterIndex, renderSenderParameter(code, parameter, senderIndex, subIndex));
                senderIndex++;
            }
        }

        code.append("        if (!rateLimiter.tryAcquire(source, COMMAND_PATH_")
                .append(subIndex)
                .append(")) {\n");
        code.append("            scheduler.execute(() -> source.sendMessage(messages.rateLimited()));\n");
        code.append(RETURN_ZERO);
        code.append(CLOSE_BRACE);

        int argumentIndex = 0;
        int flagIndex = 0;
        for (int parameterIndex = 0; parameterIndex < parameters.size(); parameterIndex++) {
            ParameterModel parameter = parameters.get(parameterIndex);
            switch (parameter.getKind()) {
                case SENDER -> {
                    /* sender parameter is injected directly by the adapter */
                }
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

    private String renderSenderParameter(StringBuilder code, ParameterModel parameter, int senderIndex, int subIndex) {
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
            code.append(
                    "            ADAPTER_HELPER_LOGGER.log(java.util.logging.Level.WARNING, () -> \"Expected sender type \"\n");
            code.append("                    + \"")
                    .append(AdapterRenderingSupport.escape(parameter.getTypeName()))
                    .append("\"\n");
            code.append("                    + \" but got \" + source.getHandle().getClass().getName()\n");
            code.append("                    + \" for command path \" + COMMAND_PATH_")
                    .append(subIndex)
                    .append(");\n");
            code.append("            scheduler.execute(() -> source.sendMessage(messages.invalidSender()));\n");
            code.append(RETURN_ZERO);
            code.append(CLOSE_BRACE);
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
            case JAVA_STRING,
                    JAVA_STRING_ARRAY,
                    JAVA_STRING_LIST,
                    "int",
                    JAVA_INT_OBJ,
                    "long",
                    JAVA_LONG_OBJ,
                    TYPE_FLOAT,
                    JAVA_FLOAT_OBJ,
                    TYPE_DOUBLE,
                    JAVA_DOUBLE_OBJ,
                    TYPE_BOOLEAN,
                    JAVA_BOOLEAN_OBJ -> false;
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
        if (!JAVA_STRING.equals(parameter.getTypeName())
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
        code.append(RETURN_ZERO);
        code.append(CLOSE_BRACE);
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
        code.append(CLOSE_BRACE_METHOD);
    }

    private void renderSyncVoidInvocation(StringBuilder code, String invocation, int subIndex) {
        code.append("        return toBrigadierResult(source, executor.executeSync(source, COMMAND_PATH_")
                .append(subIndex)
                .append(", () -> ")
                .append(invocation)
                .append("), scheduler);\n");
        code.append(CLOSE_BRACE_METHOD);
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
        code.append(CLOSE_BRACE);
        code.append("        return toBrigadierResult(source, commandResult, scheduler);\n");
        code.append(CLOSE_BRACE_METHOD);
    }

    private String suggestionsExpression(ParameterModel parameter, int subIndex) {
        if (parameter.getSuggestionMethodName() == null) {
            return "";
        }
        return "\n            .suggests((context, builder) -> suggestFrom(builder, telemetry, COMMAND_PATH_"
                + subIndex
                + ", suggestionExecutor, "
                + parameter.isAsyncSuggestions()
                + ", () -> instance."
                + parameter.getSuggestionMethodName()
                + "()))";
    }

    private String argumentBuilderType(ParameterModel parameter) {
        return switch (parameter.getTypeName()) {
            case "int", JAVA_INT_OBJ -> "Integer";
            case "long", JAVA_LONG_OBJ -> "Long";
            case TYPE_FLOAT, JAVA_FLOAT_OBJ -> "Float";
            case TYPE_DOUBLE, JAVA_DOUBLE_OBJ -> "Double";
            case TYPE_BOOLEAN, JAVA_BOOLEAN_OBJ -> "Boolean";
            case JAVA_STRING -> "String";
            case JAVA_STRING_ARRAY, JAVA_STRING_LIST -> "String";
            default -> "Object";
        };
    }

    private String argumentTypeExpression(ParameterModel parameter) {
        return switch (parameter.getTypeName()) {
            case JAVA_STRING_ARRAY, JAVA_STRING_LIST -> "StringArgumentType.greedyString()";
            case JAVA_STRING ->
                parameter.isGreedy() ? "StringArgumentType.greedyString()" : "StringArgumentType.string()";
            case "int", JAVA_INT_OBJ -> numericArgumentType("IntegerArgumentType.integer", parameter, "int");
            case "long", JAVA_LONG_OBJ -> numericArgumentType("LongArgumentType.longArg", parameter, "long");
            case TYPE_FLOAT, JAVA_FLOAT_OBJ -> numericArgumentType("FloatArgumentType.floatArg", parameter, TYPE_FLOAT);
            case TYPE_DOUBLE, JAVA_DOUBLE_OBJ ->
                numericArgumentType("DoubleArgumentType.doubleArg", parameter, TYPE_DOUBLE);
            case TYPE_BOOLEAN, JAVA_BOOLEAN_OBJ -> "BoolArgumentType.bool()";
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
            case JAVA_STRING -> "stripFormattingCodes(StringArgumentType.getString(context, " + argumentName + "))";
            case JAVA_STRING_ARRAY ->
                "splitArguments(StringArgumentType.getString(context, " + argumentName + ")).toArray(String[]::new)";
            case JAVA_STRING_LIST -> "splitArguments(StringArgumentType.getString(context, " + argumentName + "))";
            case "int", JAVA_INT_OBJ -> "IntegerArgumentType.getInteger(context, " + argumentName + ")";
            case "long", JAVA_LONG_OBJ -> "LongArgumentType.getLong(context, " + argumentName + ")";
            case TYPE_FLOAT, JAVA_FLOAT_OBJ -> "FloatArgumentType.getFloat(context, " + argumentName + ")";
            case TYPE_DOUBLE, JAVA_DOUBLE_OBJ -> "DoubleArgumentType.getDouble(context, " + argumentName + ")";
            case TYPE_BOOLEAN, JAVA_BOOLEAN_OBJ -> "BoolArgumentType.getBool(context, " + argumentName + ")";
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
            case JAVA_STRING -> "\"" + AdapterRenderingSupport.escape(value == null ? "" : value) + "\"";
            case JAVA_STRING_ARRAY ->
                "splitArguments(\""
                        + AdapterRenderingSupport.escape(value == null ? "" : value)
                        + "\").toArray(String[]::new)";
            case JAVA_STRING_LIST ->
                "splitArguments(\"" + AdapterRenderingSupport.escape(value == null ? "" : value) + "\")";
            case "int", JAVA_INT_OBJ -> Integer.toString(parseDefaultInt(value));
            case "long", JAVA_LONG_OBJ -> Long.toString(parseDefaultLong(value)) + "L";
            case TYPE_FLOAT, JAVA_FLOAT_OBJ -> Float.toString(parseDefaultFloat(value)) + "F";
            case TYPE_DOUBLE, JAVA_DOUBLE_OBJ -> Double.toString(parseDefaultDouble(value));
            case TYPE_BOOLEAN, JAVA_BOOLEAN_OBJ -> Boolean.toString(Boolean.parseBoolean(value));
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
