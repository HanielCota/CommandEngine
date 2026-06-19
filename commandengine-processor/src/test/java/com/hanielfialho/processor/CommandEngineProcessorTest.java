package com.hanielfialho.processor;

import static com.google.testing.compile.CompilationSubject.assertThat;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.Test;

final class CommandEngineProcessorTest {

    @Test
    void generatesBrigadierAdapterForStringArgumentSubcommand() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.GuildCommand", """
                package example;

                import com.hanielfialho.api.annotation.Arg;
                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Execute;
                import com.hanielfialho.api.annotation.Sender;
                import com.hanielfialho.api.annotation.Subcommand;
                import com.hanielfialho.api.source.CommandSource;

                @Command(name = "guild", permission = "guild.use")
                public final class GuildCommand {

                    @Subcommand("create")
                    @Execute
                    public void create(@Sender CommandSource source, @Arg("name") String name) {
                        source.sendMessage(name);
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("example.GuildCommandCommandAdapter")
                .contentsAsUtf8String()
                .contains("StringArgumentType.getString(context, \"name\")");
        assertThat(compilation)
                .generatedSourceFile("example.GuildCommandCommandAdapterFactory")
                .contentsAsUtf8String()
                .contains("implements CommandAdapterFactory<GuildCommand>");
    }

    @Test
    void generatesNumericArgumentWithRange() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.LevelCommand", """
                package example;

                import com.hanielfialho.api.annotation.Arg;
                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Range;
                import com.hanielfialho.api.annotation.Subcommand;

                @Command(name = "level")
                public final class LevelCommand {

                    @Subcommand("set")
                    public void set(@Arg("amount") @Range(min = 1, max = 100) int amount) {
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).succeeded();
        var generatedAdapter = assertThat(compilation)
                .generatedSourceFile("example.LevelCommandCommandAdapter")
                .contentsAsUtf8String();
        generatedAdapter.contains("IntegerArgumentType.integer(1, 100)");
        generatedAdapter.contains("IntegerArgumentType.getInteger(context, \"amount\")");
    }

    @Test
    void generatesOptionalArgumentDefaultAndGreedyString() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.MessageCommand", """
                package example;

                import com.hanielfialho.api.annotation.Arg;
                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Greedy;
                import com.hanielfialho.api.annotation.Optional;
                import com.hanielfialho.api.annotation.Subcommand;

                @Command(name = "message")
                public final class MessageCommand {

                    @Subcommand("send")
                    public void send(@Arg("body") @Optional(defaultValue = "hello") @Greedy String body) {
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).succeeded();
        var generatedAdapter = assertThat(compilation)
                .generatedSourceFile("example.MessageCommandCommandAdapter")
                .contentsAsUtf8String();
        generatedAdapter.contains("StringArgumentType.greedyString()");
        generatedAdapter.contains(
                "hasArgument(context, \"body\") ? stripFormattingCodes(StringArgumentType.getString(context, \"body\")) : \"hello\"");
        generatedAdapter.contains("parsedNode.getNode() instanceof ArgumentCommandNode");
        generatedAdapter.doesNotContain("context.getArgument(name, Object.class)");
    }

    @Test
    void generatesOptionalLiteralFlag() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.DebugCommand", """
                package example;

                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Flag;
                import com.hanielfialho.api.annotation.Subcommand;

                @Command(name = "debug")
                public final class DebugCommand {

                    @Subcommand("toggle")
                    public void toggle(@Flag(value = "verbose", shorthand = 'v') boolean verbose) {
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).succeeded();
        var generatedAdapter = assertThat(compilation)
                .generatedSourceFile("example.DebugCommandCommandAdapter")
                .contentsAsUtf8String();
        generatedAdapter.contains("literal(\"--verbose\")");
        generatedAdapter.contains("literal(\"-v\")");
        generatedAdapter.contains("hasFlag(context, \"verbose\", 'v')");
        generatedAdapter.contains("for (var parsedNode : context.getNodes())");
        generatedAdapter.doesNotContain("context.getInput()");
    }

    @Test
    void generatesVoidHandlersAsAsyncByDefaultAndAllowsSyncOptOut() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.TaskCommand", """
                package example;

                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Execute;
                import com.hanielfialho.api.annotation.Subcommand;

                @Command(name = "task")
                public final class TaskCommand {

                    @Subcommand("run")
                    public void run() {
                    }

                    @Subcommand("sync")
                    @Execute(async = false)
                    public void sync() {
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).succeeded();
        var generatedAdapter = assertThat(compilation)
                .generatedSourceFile("example.TaskCommandCommandAdapter")
                .contentsAsUtf8String();
        generatedAdapter.contains("executor.executeAsync(source, COMMAND_PATH_0, () -> instance.run())");
        generatedAdapter.contains("executor.executeSync(source, COMMAND_PATH_1, () -> instance.sync())");
    }

    @Test
    void rejectsAsyncHandlersThatReturnBrigadierResult() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.TaskCommand", """
                package example;

                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Execute;
                import com.hanielfialho.api.annotation.Subcommand;

                @Command(name = "task")
                public final class TaskCommand {

                    @Subcommand("run")
                    @Execute(async = true)
                    public int run() {
                        return 1;
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("@Execute(async = true) requires a void command handler");
    }

    @Test
    void generatesResolverLookupForExternalArgumentType() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.BadCommand", """
                package example;

                import com.hanielfialho.api.annotation.Arg;
                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Subcommand;
                import java.time.Instant;

                @Command(name = "bad")
                public final class BadCommand {

                    @Subcommand("run")
                    public void run(@Arg("instant") Instant instant) {
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("example.BadCommandCommandAdapter")
                .contentsAsUtf8String()
                .contains("resolveArgument(context, \"instant\", java.time.Instant.class)");
    }

    @Test
    void generatesLazySuggestionsFromProviderMethod() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.WarpCommand", """
                package example;

                import com.hanielfialho.api.annotation.Arg;
                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.SuggestionProvider;
                import com.hanielfialho.api.annotation.Suggestions;
                import com.hanielfialho.api.annotation.Subcommand;
                import java.util.List;

                @Command(name = "warp")
                public final class WarpCommand {

                    @Subcommand("teleport")
                    public void teleport(@Arg("name") @Suggestions("warpNames") String warpName) {
                    }

                    @SuggestionProvider("warpNames")
                    public List<String> warpNames() {
                        return List.of("spawn", "shop");
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).succeeded();
        var generatedAdapter = assertThat(compilation)
                .generatedSourceFile("example.WarpCommandCommandAdapter")
                .contentsAsUtf8String();
        generatedAdapter.contains(
                ".suggests((context, builder) -> suggestFrom(builder, telemetry, COMMAND_PATH_0, () -> instance.warpNames()))");
        generatedAdapter.contains("long started = telemetry == CommandTelemetry.NOOP ? 0L : System.nanoTime()");
        generatedAdapter.contains("if (telemetry == CommandTelemetry.NOOP)");
    }

    @Test
    void rejectsMissingSuggestionProvider() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.WarpCommand", """
                package example;

                import com.hanielfialho.api.annotation.Arg;
                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Suggestions;
                import com.hanielfialho.api.annotation.Subcommand;

                @Command(name = "warp")
                public final class WarpCommand {

                    @Subcommand("teleport")
                    public void teleport(@Arg("name") @Suggestions("missing") String warpName) {
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("No @SuggestionProvider found for suggestions: missing");
    }

    @Test
    void inheritsCommandPermissionForSubcommand() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.GuildCommand", """
                package example;

                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Subcommand;

                @Command(name = "guild", permission = "guild.use")
                public final class GuildCommand {

                    @Subcommand("home")
                    public void home() {
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("example.GuildCommandCommandAdapter")
                .contentsAsUtf8String()
                .contains("new SubcommandMetadata(\"home\", \"guild.use\"");
    }

    @Test
    void registersAliasesAndUnregistersThem() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.TeleportCommand", """
                package example;

                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Subcommand;

                @Command(name = "teleport", aliases = {"tp", "tpa"})
                public final class TeleportCommand {

                    @Subcommand("player")
                    public void player() {
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).succeeded();
        var generatedAdapter = assertThat(compilation)
                .generatedSourceFile("example.TeleportCommandCommandAdapter")
                .contentsAsUtf8String();
        generatedAdapter.contains("LiteralCommandNode<CommandSource> root = createTree(\"teleport\").build()");
        generatedAdapter.contains("LiteralCommandNode<CommandSource> alias0 = createTree(\"tp\").build()");
        generatedAdapter.doesNotContain("addChild(child)");
        generatedAdapter.contains("brigadier.register(alias0, metadata())");
        generatedAdapter.contains("registeredNames.add(\"tp\")");
        generatedAdapter.contains("registeredNames.add(\"tpa\")");
    }

    @Test
    void generatesVariadicStringArgumentsForArrayAndList() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.RawCommand", """
                package example;

                import com.hanielfialho.api.annotation.Arg;
                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Subcommand;
                import java.util.List;

                @Command(name = "raw")
                public final class RawCommand {

                    @Subcommand("array")
                    public void array(@Arg("args") String[] args) {
                    }

                    @Subcommand("list")
                    public void list(@Arg("args") List<String> args) {
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).succeeded();
        var generatedAdapter = assertThat(compilation)
                .generatedSourceFile("example.RawCommandCommandAdapter")
                .contentsAsUtf8String();
        generatedAdapter.contains("StringArgumentType.greedyString()");
        generatedAdapter.contains(
                "splitArguments(StringArgumentType.getString(context, \"args\")).toArray(String[]::new)");
        generatedAdapter.contains(
                "java.util.List<java.lang.String> arg0 = splitArguments(StringArgumentType.getString(context, \"args\"))");
        generatedAdapter.contains("List<String> arguments = new java.util.ArrayList<>()");
        generatedAdapter.doesNotContain("split(\"\\\\s+\")");
        generatedAdapter.contains("new ParameterMetadata(\"args\", java.util.List.class");
    }

    @Test
    void generatesConventionalRootHandlerWithoutParameterAnnotations() {
        JavaFileObject player = JavaFileObjects.forSourceString("example.Player", """
                package example;

                public interface Player {
                }
                """);
        JavaFileObject command = JavaFileObjects.forSourceString("example.RawCommand", """
                package example;

                import com.hanielfialho.api.annotation.Command;

                @Command(name = "raw", aliases = {"r"})
                public final class RawCommand {

                    public void onCommand(Player player, String args[]) {
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(player, command);

        assertThat(compilation).succeeded();
        var generatedAdapter = assertThat(compilation)
                .generatedSourceFile("example.RawCommandCommandAdapter")
                .contentsAsUtf8String();
        generatedAdapter.contains("argument0_0.executes(this::execute0)");
        generatedAdapter.contains("root.then(argument0_0)");
        generatedAdapter.contains("if (!(source.getHandle() instanceof example.Player sender0))");
        generatedAdapter.contains("scheduler.execute(() -> source.sendMessage(messages.invalidSender()))");
        generatedAdapter.contains(
                "java.lang.String[] arg0 = splitArguments(StringArgumentType.getString(context, \"args\")).toArray(String[]::new)");
        generatedAdapter.contains("LiteralCommandNode<CommandSource> alias0 = createTree(\"r\").build()");
    }

    @Test
    void generatedAdapterDoesNotExposeRuntimeExceptionMessages() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.FailingCommand", """
                package example;

                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Subcommand;

                @Command(name = "fail")
                public final class FailingCommand {

                    @Subcommand("void")
                    public void failVoid() {
                        throw new IllegalStateException("secret-token");
                    }

                    @Subcommand("int")
                    public int failInt() {
                        throw new IllegalStateException("secret-token");
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).succeeded();
        var generatedAdapter = assertThat(compilation)
                .generatedSourceFile("example.FailingCommandCommandAdapter")
                .contentsAsUtf8String();
        generatedAdapter.contains("CommandResult.failure(FailureReason.EXCEPTION, DEFAULT_MESSAGES.internalError())");
        generatedAdapter.contains("CommandResult commandResult = executor.executeSync(source, COMMAND_PATH_1");
        generatedAdapter.doesNotContain("exception.getMessage()");
    }

    @Test
    void convertsNegativeIntReturnToFailure() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.NegativeCommand", """
                package example;

                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Subcommand;

                @Command(name = "negative")
                public final class NegativeCommand {

                    @Subcommand("value")
                    public int value() {
                        return -1;
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("example.NegativeCommandCommandAdapter")
                .contentsAsUtf8String()
                .contains("result[0] < 0 ? CommandResult.failure(FailureReason.EXCEPTION, messages.internalError())");
    }

    @Test
    void appliesArgNumericRangeAndStringLength() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.ValidatedCommand", """
                package example;

                import com.hanielfialho.api.annotation.Arg;
                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Subcommand;

                @Command(name = "validated")
                public final class ValidatedCommand {

                    @Subcommand("name")
                    public void name(@Arg(value = "name", minLength = 3, maxLength = 16) String name) {
                    }

                    @Subcommand("amount")
                    public void amount(@Arg(value = "amount", min = 1, max = 100) int amount) {
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).succeeded();
        var generatedAdapter = assertThat(compilation)
                .generatedSourceFile("example.ValidatedCommandCommandAdapter")
                .contentsAsUtf8String();
        generatedAdapter.contains("arg0.length() < 3 || arg0.length() > 16");
        generatedAdapter.contains("IntegerArgumentType.integer(1, 100)");
    }

    @Test
    void rejectsTooManyFlagsBeforeGeneratingCombinatorialTree() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.FlagCommand", """
                package example;

                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Flag;
                import com.hanielfialho.api.annotation.Subcommand;

                @Command(name = "flags")
                public final class FlagCommand {

                    @Subcommand("run")
                    public void run(
                            @Flag("a") boolean a,
                            @Flag("b") boolean b,
                            @Flag("c") boolean c,
                            @Flag("d") boolean d,
                            @Flag("e") boolean e,
                            @Flag("f") boolean f) {
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Subcommands support at most 5 @Flag parameters");
    }

    @Test
    void rejectsInaccessibleSuggestionProviders() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.WarpCommand", """
                package example;

                import com.hanielfialho.api.annotation.Arg;
                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.SuggestionProvider;
                import com.hanielfialho.api.annotation.Suggestions;
                import com.hanielfialho.api.annotation.Subcommand;
                import java.util.List;

                @Command(name = "warp")
                public final class WarpCommand {

                    @Subcommand("teleport")
                    public void teleport(@Arg("name") @Suggestions("warpNames") String warpName) {
                    }

                    @SuggestionProvider("warpNames")
                    private static List<String> warpNames() {
                        return List.of("spawn");
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).failed();
        assertThat(compilation)
                .hadErrorContaining(
                        "@SuggestionProvider methods must be instance methods accessible to the generated adapter");
    }

    @Test
    void rejectsDuplicateSubcommandPaths() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.DuplicateCommand", """
                package example;

                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Subcommand;

                @Command(name = "duplicate")
                public final class DuplicateCommand {

                    @Subcommand("run")
                    public void first() {
                    }

                    @Subcommand("run")
                    public void second() {
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Duplicate @Subcommand path: run");
    }

    @Test
    void rejectsGreedyArgumentsCombinedWithFlags() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.BroadcastCommand", """
                package example;

                import com.hanielfialho.api.annotation.Arg;
                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Flag;
                import com.hanielfialho.api.annotation.Greedy;
                import com.hanielfialho.api.annotation.Subcommand;

                @Command(name = "broadcast")
                public final class BroadcastCommand {

                    @Subcommand("send")
                    public void send(@Flag("silent") boolean silent, @Arg("message") @Greedy String message) {
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("@Greedy arguments cannot be combined with @Flag parameters");
    }

    @Test
    void rejectsBlankCommandAliases() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.BadCommand", """
                package example;

                import com.hanielfialho.api.annotation.Command;

                @Command(name = "bad", aliases = {""})
                public final class BadCommand {
                    public void onCommand() {
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("@Command aliases must not contain blank values");
    }

    @Test
    void rejectsBlankArgumentAndFlagNames() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.BadCommand", """
                package example;

                import com.hanielfialho.api.annotation.Arg;
                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Flag;
                import com.hanielfialho.api.annotation.Subcommand;

                @Command(name = "bad")
                public final class BadCommand {

                    @Subcommand("arg")
                    public void arg(@Arg("") String name) {
                    }

                    @Subcommand("flag")
                    public void flag(@Flag("") boolean verbose) {
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("@Arg value must not be blank");
        assertThat(compilation).hadErrorContaining("@Flag value must not be blank");
    }

    @Test
    void rejectsOptionalArgumentBeforeRequiredArgument() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.BadCommand", """
                package example;

                import com.hanielfialho.api.annotation.Arg;
                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Optional;
                import com.hanielfialho.api.annotation.Subcommand;

                @Command(name = "bad")
                public final class BadCommand {

                    @Subcommand("run")
                    public void run(@Arg("first") @Optional String first, @Arg("second") String second) {
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("@Optional arguments must come after required arguments");
    }

    @Test
    void rejectsDuplicateSuggestionProviderNames() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.WarpCommand", """
                package example;

                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.SuggestionProvider;
                import java.util.List;

                @Command(name = "warp")
                public final class WarpCommand {
                    public void onCommand() {
                    }

                    @SuggestionProvider("names")
                    public List<String> first() {
                        return List.of("spawn");
                    }

                    @SuggestionProvider("names")
                    public List<String> second() {
                        return List.of("shop");
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Duplicate @SuggestionProvider value: names");
    }

    @Test
    void rejectsSuggestionProvidersThatDoNotReturnStringLists() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.WarpCommand", """
                package example;

                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.SuggestionProvider;
                import java.util.List;

                @Command(name = "warp")
                public final class WarpCommand {
                    public void onCommand() {
                    }

                    @SuggestionProvider("names")
                    public List<Integer> names() {
                        return List.of(1);
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("@SuggestionProvider methods must return java.util.List<String>");
    }

    @Test
    void rejectsSubcommandWithInvalidReturnType() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.BadCommand", """
                package example;

                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Subcommand;

                @Command(name = "bad")
                public final class BadCommand {

                    @Subcommand("run")
                    public String run() {
                        return "ok";
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("@Subcommand handler must return void or int");
    }

    @Test
    void rejectsPrivateSubcommandHandler() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.BadCommand", """
                package example;

                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Subcommand;

                @Command(name = "bad")
                public final class BadCommand {

                    @Subcommand("run")
                    private void run() {
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("@Subcommand handler must not be private");
    }

    @Test
    void rejectsStaticSubcommandHandler() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.BadCommand", """
                package example;

                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Subcommand;

                @Command(name = "bad")
                public final class BadCommand {

                    @Subcommand("run")
                    public static void run() {
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("@Subcommand handler must not be static");
    }

    @Test
    void rejectsAliasEqualToCommandName() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.BadCommand", """
                package example;

                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Subcommand;

                @Command(name = "bad", aliases = {"bad"})
                public final class BadCommand {

                    @Subcommand("run")
                    public void run() {
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("@Command alias must not be equal to the command name");
    }

    @Test
    void rejectsAliasEqualToCommandNameIgnoringCase() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.BadCommand", """
                package example;

                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Subcommand;

                @Command(name = "bad", aliases = {"BAD"})
                public final class BadCommand {

                    @Subcommand("run")
                    public void run() {
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("@Command alias must not be equal to the command name");
    }

    @Test
    void rejectsDuplicateAliasesIgnoringCase() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.BadCommand", """
                package example;

                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Subcommand;

                @Command(name = "bad", aliases = {"Run", "run"})
                public final class BadCommand {

                    @Subcommand("execute")
                    public void execute() {
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Duplicate @Command alias");
    }

    @Test
    void rejectsInvalidOptionalDefaultValue() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.BadCommand", """
                package example;

                import com.hanielfialho.api.annotation.Arg;
                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Optional;
                import com.hanielfialho.api.annotation.Subcommand;

                @Command(name = "bad")
                public final class BadCommand {

                    @Subcommand("run")
                    public void run(@Arg("amount") @Optional(defaultValue = "abc") int amount) {
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Invalid default value \"abc\" for type int");
    }

    @Test
    void appliesSuggestionsOnInferredArgumentParameter() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.SuggestCommand", """
                package example;

                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.SuggestionProvider;
                import com.hanielfialho.api.annotation.Suggestions;
                import com.hanielfialho.api.annotation.Subcommand;
                import java.util.List;

                @Command(name = "suggest")
                public final class SuggestCommand {

                    @Subcommand("name")
                    public void name(@Suggestions("names") String name) {
                    }

                    @SuggestionProvider("names")
                    public List<String> names() {
                        return List.of("alice", "bob");
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).succeeded();
        var generatedAdapter = assertThat(compilation)
                .generatedSourceFile("example.SuggestCommandCommandAdapter")
                .contentsAsUtf8String();
        generatedAdapter.contains(
                ".suggests((context, builder) -> suggestFrom(builder, telemetry, COMMAND_PATH_0, () -> instance.names()))");
    }

    @Test
    void rejectsOptionalOnCustomType() {
        JavaFileObject player = JavaFileObjects.forSourceString("example.Player", """
                package example;

                public interface Player {
                }
                """);
        JavaFileObject command = JavaFileObjects.forSourceString("example.BadCommand", """
                package example;

                import com.hanielfialho.api.annotation.Arg;
                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Optional;
                import com.hanielfialho.api.annotation.Subcommand;

                @Command(name = "bad")
                public final class BadCommand {

                    @Subcommand("run")
                    public void run(@Arg("target") @Optional Player target) {
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(player, command);

        assertThat(compilation).failed();
        assertThat(compilation)
                .hadErrorContaining("@Optional is only supported for built-in argument types and string sequences");
    }

    @Test
    void rejectsSenderPrimitiveParameter() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.BadCommand", """
                package example;

                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Sender;
                import com.hanielfialho.api.annotation.Subcommand;

                @Command(name = "bad")
                public final class BadCommand {

                    @Subcommand("run")
                    public void run(@Sender int source) {
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("@Sender parameters must not be primitive types");
    }

    @Test
    void rejectsOptionalOnSenderParameter() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.BadCommand", """
                package example;

                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Optional;
                import com.hanielfialho.api.annotation.Sender;
                import com.hanielfialho.api.annotation.Subcommand;
                import com.hanielfialho.api.source.CommandSource;

                @Command(name = "bad")
                public final class BadCommand {

                    @Subcommand("run")
                    public void run(@Sender @Optional CommandSource source) {
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("@Optional and @Greedy are not allowed on @Sender parameters");
    }

    @Test
    void rejectsCheckedExceptionOnSubcommandHandler() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.BadCommand", """
                package example;

                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Subcommand;

                @Command(name = "bad")
                public final class BadCommand {

                    @Subcommand("run")
                    public void run() throws Exception {
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("@Subcommand handler must not declare checked exceptions");
    }

    @Test
    void rejectsDuplicateFlagShorthand() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.BadCommand", """
                package example;

                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Flag;
                import com.hanielfialho.api.annotation.Subcommand;

                @Command(name = "bad")
                public final class BadCommand {

                    @Subcommand("run")
                    public void run(@Flag(value = "alpha", shorthand = 'a') boolean alpha,
                                    @Flag(value = "beta", shorthand = 'a') boolean beta) {
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Duplicate @Flag shorthand");
    }

    @Test
    void rejectsInvalidBooleanOptionalDefaultValue() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.BadCommand", """
                package example;

                import com.hanielfialho.api.annotation.Arg;
                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Optional;
                import com.hanielfialho.api.annotation.Subcommand;

                @Command(name = "bad")
                public final class BadCommand {

                    @Subcommand("run")
                    public void run(@Arg("enabled") @Optional(defaultValue = "yes") boolean enabled) {
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("@Optional defaultValue for boolean must be 'true' or 'false'");
    }

    @Test
    void rejectsInvalidCommandName() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.BadCommand", """
                package example;

                import com.hanielfialho.api.annotation.Command;

                @Command(name = "bad command")
                public final class BadCommand {
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("@Command name contains invalid characters");
    }

    @Test
    void rejectsCommandWithoutHandlers() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.EmptyCommand", """
                package example;

                import com.hanielfialho.api.annotation.Command;

                @Command(name = "empty")
                public final class EmptyCommand {
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).failed();
        assertThat(compilation)
                .hadErrorContaining("@Command classes must declare at least one @Subcommand or onCommand handler");
    }
}
