package com.hanielfialho.processor.generator;

import com.hanielfialho.processor.model.CommandModel;

final class AdapterHelperRenderer {

    private final CommandModel model;

    AdapterHelperRenderer(CommandModel model) {
        this.model = model;
    }

    void render(StringBuilder code) {
        code.append("    private static boolean hasArgument(CommandContext<CommandSource> context, String name) {\n");
        code.append("        try {\n");
        code.append("            context.getArgument(name, Object.class);\n");
        code.append("            return true;\n");
        code.append("        } catch (IllegalArgumentException exception) {\n");
        code.append("            return false;\n");
        code.append("        }\n");
        code.append("    }\n\n");

        code.append(
                "    private static boolean hasFlag(CommandContext<CommandSource> context, String name, char shorthand) {\n");
        code.append("        String input = context.getInput();\n");
        code.append("        return containsToken(input, \"--\" + name)\n");
        code.append("            || (shorthand != '\\0' && containsToken(input, \"-\" + shorthand));\n");
        code.append("    }\n\n");

        code.append("    private static boolean containsToken(String input, String expected) {\n");
        code.append("        for (String token : input.trim().split(\"\\\\s+\")) {\n");
        code.append("            if (token.equals(expected)) {\n");
        code.append("                return true;\n");
        code.append("            }\n");
        code.append("        }\n");
        code.append("        return false;\n");
        code.append("    }\n\n");

        code.append("    private static List<String> splitArguments(String input) {\n");
        code.append("        if (input == null || input.isBlank()) {\n");
        code.append("            return List.of();\n");
        code.append("        }\n");
        code.append("        return List.of(input.trim().split(\"\\\\s+\"));\n");
        code.append("    }\n\n");

        renderSuggestionHelper(code);
        renderResolverHelpers(code);
        renderResultHelpers(code);
    }

    private void renderSuggestionHelper(StringBuilder code) {
        code.append(
                "    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestFrom(\n");
        code.append("            SuggestionsBuilder builder, List<String> suggestions) {\n");
        code.append("        if (suggestions == null || suggestions.isEmpty()) {\n");
        code.append("            return builder.buildFuture();\n");
        code.append("        }\n");
        code.append("        String remaining = builder.getRemainingLowerCase();\n");
        code.append("        for (String suggestion : suggestions) {\n");
        code.append(
                "            if (suggestion != null && suggestion.toLowerCase(java.util.Locale.ROOT).startsWith(remaining)) {\n");
        code.append("                builder.suggest(suggestion);\n");
        code.append("            }\n");
        code.append("        }\n");
        code.append("        return builder.buildFuture();\n");
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
        code.append("        return resolverFor(type).resolveDefault(context, input);\n");
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
        code.append("        scheduler.execute(() -> source.sendMessage(messages.internalError()));\n");
        code.append("        return null;\n");
        code.append("    }\n\n");
    }
}
