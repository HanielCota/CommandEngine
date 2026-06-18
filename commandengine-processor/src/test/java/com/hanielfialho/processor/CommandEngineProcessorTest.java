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
                "hasArgument(context, \"body\") ? StringArgumentType.getString(context, \"body\") : \"hello\"");
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
        generatedAdapter.contains(".suggests((context, builder) -> suggestFrom(builder, instance.warpNames()))");
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
        generatedAdapter.contains(
                "LiteralArgumentBuilder<CommandSource> aliasBuilder0 = LiteralArgumentBuilder.<CommandSource>literal(\"tp\")");
        generatedAdapter.contains("for (CommandNode<CommandSource> child : root.getChildren())");
        generatedAdapter.contains("alias0.addChild(child)");
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
        generatedAdapter.contains("java.util.List<java.lang.String> arg0 = hasArgument(context, \"args\")");
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
        generatedAdapter.contains("root.executes(this::execute0)");
        generatedAdapter.contains("if (!(source.getHandle() instanceof example.Player sender0))");
        generatedAdapter.contains("scheduler.execute(() -> source.sendMessage(messages.invalidSender()))");
        generatedAdapter.contains("java.lang.String[] arg0 = hasArgument(context, \"args\")");
        generatedAdapter.contains("aliasBuilder0.executes(root.getCommand())");
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
        generatedAdapter.contains("CommandResult.failure(FailureReason.EXCEPTION, messages.internalError())");
        generatedAdapter.doesNotContain("exception.getMessage()");
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
}
