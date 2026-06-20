package com.hanielfialho.processor.generator;

import com.hanielfialho.processor.model.CommandModel;
import com.hanielfialho.processor.model.SubcommandModel;

final class AdapterMemberRenderer {

    private final CommandModel model;

    AdapterMemberRenderer(CommandModel model) {
        this.model = model;
    }

    void render(StringBuilder code, String adapterName) {
        code.append("@SuppressWarnings({\"all\", \"NullableProblems\"})\n");
        code.append("public final class ").append(adapterName).append(" implements CommandAdapter {\n\n");
        code.append("    private static final CommandMessages DEFAULT_MESSAGES = CommandMessages.defaults();\n");
        renderCommandPathConstants(code);
        code.append("\n");
        renderDirectExecutor(code);
        renderFields(code);
        renderConstructors(code, adapterName);
    }

    private void renderCommandPathConstants(StringBuilder code) {
        for (int subIndex = 0; subIndex < model.getSubcommands().size(); subIndex++) {
            SubcommandModel subcommand = model.getSubcommands().get(subIndex);
            code.append("    private static final CommandPath COMMAND_PATH_")
                    .append(subIndex)
                    .append(" = new CommandPath(")
                    .append(AdapterRenderingSupport.commandPathArguments(model, subcommand))
                    .append(");\n");
        }
    }

    private void renderDirectExecutor(StringBuilder code) {
        code.append("    private static final CommandExecutor DIRECT_EXECUTOR = new CommandExecutor() {\n");
        code.append("        @Override\n");
        code.append("        public CommandResult executeSync(CommandSource source, Runnable command) {\n");
        code.append("            try {\n");
        code.append("                command.run();\n");
        code.append("                return CommandResult.success();\n");
        code.append("            } catch (RuntimeException exception) {\n");
        code.append(
                "                return CommandResult.failure(FailureReason.EXCEPTION, DEFAULT_MESSAGES.internalError());\n");
        code.append("            }\n");
        code.append("        }\n\n");
        code.append("        @Override\n");
        code.append(
                "        public CompletableFuture<CommandResult> executeAsync(CommandSource source, Runnable command) {\n");
        code.append("            return CompletableFuture.completedFuture(executeSync(source, command));\n");
        code.append("        }\n");
        code.append("    };\n\n");
    }

    private void renderFields(StringBuilder code) {
        code.append("    private final ").append(model.getSimpleClassName()).append(" instance;\n");
        code.append("    private final CommandExecutor executor;\n");
        code.append("    private final ArgumentResolverRegistry argumentResolvers;\n");
        code.append("    private final CommandScheduler scheduler;\n");
        code.append("    private final CommandMessages messages;\n");
        code.append("    private final CommandTelemetry telemetry;\n");
        code.append("    private final CommandRateLimiter rateLimiter;\n");
        code.append("    private final SuggestionExecutor suggestionExecutor;\n");
        code.append(
                "    private final List<String> registeredNames = new java.util.concurrent.CopyOnWriteArrayList<>();\n\n");
    }

    private void renderConstructors(StringBuilder code, String adapterName) {
        code.append("    public ")
                .append(adapterName)
                .append("(")
                .append(model.getSimpleClassName())
                .append(" instance) {\n");
        code.append("        this(instance, DIRECT_EXECUTOR, null);\n");
        code.append("    }\n\n");
        code.append("    public ")
                .append(adapterName)
                .append("(")
                .append(model.getSimpleClassName())
                .append(" instance, CommandExecutor executor) {\n");
        code.append("        this(instance, executor, null);\n");
        code.append("    }\n\n");
        code.append("    public ")
                .append(adapterName)
                .append("(")
                .append(model.getSimpleClassName())
                .append(" instance, CommandExecutor executor, ArgumentResolverRegistry argumentResolvers) {\n");
        code.append(
                "        this(instance, executor, argumentResolvers, CommandScheduler.DIRECT, DEFAULT_MESSAGES, CommandTelemetry.NOOP, CommandRateLimiter.NONE, SuggestionExecutor.DIRECT);\n");
        code.append("    }\n\n");
        code.append("    public ")
                .append(adapterName)
                .append("(")
                .append(model.getSimpleClassName())
                .append(" instance, CommandExecutor executor, ArgumentResolverRegistry argumentResolvers, ")
                .append(
                        "CommandScheduler scheduler, CommandMessages messages, CommandTelemetry telemetry, CommandRateLimiter rateLimiter) {\n");
        code.append(
                "        this(instance, executor, argumentResolvers, scheduler, messages, telemetry, rateLimiter, SuggestionExecutor.DIRECT);\n");
        code.append("    }\n\n");

        code.append("    public ")
                .append(adapterName)
                .append("(")
                .append(model.getSimpleClassName())
                .append(" instance, CommandExecutor executor, ArgumentResolverRegistry argumentResolvers, ")
                .append(
                        "CommandScheduler scheduler, CommandMessages messages, CommandTelemetry telemetry, CommandRateLimiter rateLimiter, SuggestionExecutor suggestionExecutor) {\n");
        code.append("        this.instance = Objects.requireNonNull(instance, \"instance\");\n");
        code.append("        this.executor = Objects.requireNonNull(executor, \"executor\");\n");
        code.append("        this.argumentResolvers = argumentResolvers;\n");
        code.append("        this.scheduler = Objects.requireNonNull(scheduler, \"scheduler\");\n");
        code.append("        this.messages = Objects.requireNonNull(messages, \"messages\");\n");
        code.append("        this.telemetry = Objects.requireNonNull(telemetry, \"telemetry\");\n");
        code.append("        this.rateLimiter = Objects.requireNonNull(rateLimiter, \"rateLimiter\");\n");
        code.append(
                "        this.suggestionExecutor = Objects.requireNonNull(suggestionExecutor, \"suggestionExecutor\");\n");
        code.append("    }\n\n");
    }
}
