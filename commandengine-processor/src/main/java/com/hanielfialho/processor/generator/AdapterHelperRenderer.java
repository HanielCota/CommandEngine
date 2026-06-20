package com.hanielfialho.processor.generator;

import com.hanielfialho.processor.model.CommandModel;

final class AdapterHelperRenderer {

    private final CommandModel model;

    AdapterHelperRenderer(CommandModel model) {
        this.model = model;
    }

    void render(StringBuilder code) {
        code.append("    private static boolean hasArgument(CommandContext<CommandSource> context, String name) {\n");
        code.append("        for (var parsedNode : context.getNodes()) {\n");
        code.append(
                "            if (parsedNode.getNode() instanceof ArgumentCommandNode && parsedNode.getNode().getName().equals(name)) {\n");
        code.append("                return true;\n");
        code.append("            }\n");
        code.append("        }\n");
        code.append("        return false;\n");
        code.append("    }\n\n");

        code.append(
                "    private static boolean hasFlag(CommandContext<CommandSource> context, String name, char shorthand) {\n");
        code.append("        String longName = \"--\" + name;\n");
        code.append("        String shortName = shorthand == '\\0' ? null : \"-\" + shorthand;\n");
        code.append("        for (var parsedNode : context.getNodes()) {\n");
        code.append("            String nodeName = parsedNode.getNode().getName();\n");
        code.append(
                "            if (nodeName.equals(longName) || (shortName != null && nodeName.equals(shortName))) {\n");
        code.append("                return true;\n");
        code.append("            }\n");
        code.append("        }\n");
        code.append("        return false;\n");
        code.append("    }\n\n");

        code.append("    private static List<String> splitArguments(String input) {\n");
        code.append("        if (input == null || input.isBlank()) {\n");
        code.append("            return List.of();\n");
        code.append("        }\n");
        code.append("        input = stripFormattingCodes(input);\n");
        code.append("        input = input.trim();\n");
        code.append("        if (input.isEmpty()) {\n");
        code.append("            return List.of();\n");
        code.append("        }\n");
        code.append("        List<String> arguments = new java.util.ArrayList<>();\n");
        code.append("        int start = -1;\n");
        code.append("        for (int index = 0; index < input.length(); index++) {\n");
        code.append("            if (Character.isWhitespace(input.charAt(index))) {\n");
        code.append("                if (start >= 0) {\n");
        code.append("                    arguments.add(input.substring(start, index));\n");
        code.append("                    start = -1;\n");
        code.append("                }\n");
        code.append("            } else if (start < 0) {\n");
        code.append("                start = index;\n");
        code.append("            }\n");
        code.append("        }\n");
        code.append("        if (start >= 0) {\n");
        code.append("            arguments.add(input.substring(start));\n");
        code.append("        }\n");
        code.append("        return List.copyOf(arguments);\n");
        code.append("    }\n\n");

        code.append("    private static String stripFormattingCodes(String input) {\n");
        code.append("        if (input == null || input.indexOf('\\u00A7') < 0) {\n");
        code.append("            return input;\n");
        code.append("        }\n");
        code.append("        StringBuilder stripped = new StringBuilder(input.length());\n");
        code.append("        for (int index = 0; index < input.length(); index++) {\n");
        code.append("            char current = input.charAt(index);\n");
        code.append("            if (current == '\\u00A7' && index + 1 < input.length()) {\n");
        code.append("                index++;\n");
        code.append("                continue;\n");
        code.append("            }\n");
        code.append("            stripped.append(current);\n");
        code.append("        }\n");
        code.append("        return stripped.toString();\n");
        code.append("    }\n\n");

        renderSuggestionHelper(code);
        renderResolverHelpers(code);
        renderResultHelpers(code);
    }

    private void renderSuggestionHelper(StringBuilder code) {
        code.append("    private static final Logger ADAPTER_HELPER_LOGGER = Logger.getLogger(\"CommandEngine\");\n\n");
        code.append(
                "    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestFrom(\n");
        code.append(
                "            SuggestionsBuilder builder, CommandTelemetry telemetry, CommandPath path, SuggestionExecutor suggestionExecutor,\n");
        code.append("            boolean async, Supplier<List<String>> supplier) {\n");
        code.append("        Objects.requireNonNull(suggestionExecutor, \"suggestionExecutor\");\n");
        code.append("        if (!async) {\n");
        code.append(
                "            return CompletableFuture.completedFuture(buildSuggestions(builder, telemetry, path, supplier));\n");
        code.append("        }\n");
        code.append(
                "        return suggestionExecutor.submit(() -> buildSuggestions(builder, telemetry, path, supplier));\n");
        code.append("    }\n\n");
        code.append("    private static com.mojang.brigadier.suggestion.Suggestions buildSuggestions(\n");
        code.append(
                "            SuggestionsBuilder builder, CommandTelemetry telemetry, CommandPath path, Supplier<List<String>> supplier) {\n");
        code.append("            long started = telemetry == CommandTelemetry.NOOP ? 0L : System.nanoTime();\n");
        code.append("            int suggestionCount = 0;\n");
        code.append("            try {\n");
        code.append("                List<String> suggestions = supplier.get();\n");
        code.append("                if (suggestions == null || suggestions.isEmpty()) {\n");
        code.append("                    recordSuggestion(telemetry, path, started, suggestionCount);\n");
        code.append("                    return builder.build();\n");
        code.append("                }\n");
        code.append("                String remaining = builder.getRemainingLowerCase();\n");
        code.append("                for (String suggestion : suggestions) {\n");
        code.append(
                "                    if (suggestion != null && suggestion.toLowerCase(java.util.Locale.ROOT).startsWith(remaining)) {\n");
        code.append("                        builder.suggest(suggestion);\n");
        code.append("                        suggestionCount++;\n");
        code.append("                    }\n");
        code.append("                }\n");
        code.append("                recordSuggestion(telemetry, path, started, suggestionCount);\n");
        code.append("            } catch (RuntimeException exception) {\n");
        code.append("                recordSuggestionFailure(telemetry, path, exception);\n");
        code.append("            }\n");
        code.append("            return builder.build();\n");
        code.append("    }\n\n");
        code.append(
                "    private static void recordSuggestion(CommandTelemetry telemetry, CommandPath path, long started, int count) {\n");
        code.append("        if (telemetry == CommandTelemetry.NOOP) {\n");
        code.append("            return;\n");
        code.append("        }\n");
        code.append("        try {\n");
        code.append("            telemetry.recordSuggestion(path, System.nanoTime() - started, count);\n");
        code.append("        } catch (RuntimeException exception) {\n");
        code.append("            ADAPTER_HELPER_LOGGER.log(Level.FINE, \"Suggestion telemetry failed\", exception);\n");
        code.append("        }\n");
        code.append("    }\n\n");
        code.append(
                "    private static void recordSuggestionFailure(CommandTelemetry telemetry, CommandPath path, RuntimeException failure) {\n");
        code.append("        if (telemetry == CommandTelemetry.NOOP) {\n");
        code.append("            return;\n");
        code.append("        }\n");
        code.append("        try {\n");
        code.append("            telemetry.recordFailure(path, \"SUGGESTION\", failure);\n");
        code.append("        } catch (RuntimeException exception) {\n");
        code.append("            ADAPTER_HELPER_LOGGER.log(Level.FINE, \"Suggestion telemetry failed\", exception);\n");
        code.append("        }\n");
        code.append("    }\n\n");
    }

    private void renderResolverHelpers(StringBuilder code) {
        code.append("    @SuppressWarnings(\"unchecked\")\n");
        code.append("    private <T> ArgumentType<T> argumentTypeFor(Class<?> type) {\n");
        code.append("        return (ArgumentType<T>) resolverFor(type).argumentType();\n");
        code.append("    }\n\n");

        code.append(
                "    private <T> T resolveArgument(CommandContext<CommandSource> context, String name, Class<T> type) {\n");
        code.append("        return resolverFor(type).resolve(context, name);\n");
        code.append("    }\n\n");

        code.append(
                "    private <T> T resolveDefaultArgument(CommandContext<CommandSource> context, Class<T> type, String input) {\n");
        code.append("        ArgumentTypeResolver<T> resolver = resolverFor(type);\n");
        code.append("        if (!resolver.supportsDefault()) {\n");
        code.append(
                "            throw new IllegalArgumentException(\"ArgumentTypeResolver for \" + type.getName() + \" does not support default values\");\n");
        code.append("        }\n");
        code.append("        return resolver.resolveDefault(context, input);\n");
        code.append("    }\n\n");

        code.append("    private <T> ArgumentTypeResolver<T> resolverFor(Class<T> type) {\n");
        code.append("        if (argumentResolvers == null) {\n");
        code.append(
                "            throw new IllegalStateException(\"No ArgumentResolverRegistry configured for generated adapter\");\n");
        code.append("        }\n");
        code.append("        return argumentResolvers.resolver(type)\n");
        code.append(
                "            .orElseThrow(() -> new IllegalArgumentException(\"No ArgumentTypeResolver registered for \" + type.getName()));\n");
        code.append("    }\n\n");
    }

    private void renderResultHelpers(StringBuilder code) {
        code.append(
                "    private static int toBrigadierResult(CommandSource source, CommandResult result, CommandScheduler scheduler) {\n");
        code.append("        handleResult(source, result, scheduler);\n");
        code.append("        if (result instanceof CommandResult.Success success) {\n");
        code.append("            return success.affected();\n");
        code.append("        }\n");
        code.append("        return 0;\n");
        code.append("    }\n\n");

        code.append(
                "    private static void handleResult(CommandSource source, CommandResult result, CommandScheduler scheduler) {\n");
        code.append(
                "        if (result instanceof CommandResult.Failure failure && failure.message() != null && !failure.message().isBlank()) {\n");
        code.append("            scheduler.execute(() -> source.sendMessage(failure.message()));\n");
        code.append("        }\n");
        code.append("    }\n\n");

        code.append(
                "    private static Void handleAsyncFailure(CommandSource source, Throwable throwable, CommandScheduler scheduler, CommandMessages messages) {\n");
        code.append(
                "        ADAPTER_HELPER_LOGGER.log(Level.WARNING, \"Async command execution failed\", throwable);\n");
        code.append("        scheduler.execute(() -> source.sendMessage(messages.internalError()));\n");
        code.append("        return null;\n");
        code.append("    }\n\n");
    }
}
