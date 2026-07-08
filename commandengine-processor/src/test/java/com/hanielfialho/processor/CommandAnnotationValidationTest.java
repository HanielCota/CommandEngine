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
package com.hanielfialho.processor;

import static com.google.testing.compile.CompilationSubject.assertThat;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.Test;

final class CommandAnnotationValidationTest {

    @Test
    void rejectsCommandWithNamespaceSeparatorInName() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.BadCommand", """
                package example;

                import com.hanielfialho.api.annotation.Command;

                @Command(name = "my:command")
                public final class BadCommand {
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).failed();
    }

    @Test
    void rejectsEmptyCommandName() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.BadCommand", """
                package example;

                import com.hanielfialho.api.annotation.Command;

                @Command(name = "")
                public final class BadCommand {
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).failed();
    }

    @Test
    void generatesAdapterForCommandWithAliases() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.AliasedCommand", """
                package example;

                import com.hanielfialho.api.annotation.Arg;
                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Execute;
                import com.hanielfialho.api.annotation.Sender;
                import com.hanielfialho.api.annotation.Subcommand;
                import com.hanielfialho.api.source.CommandSource;

                @Command(name = "mycommand", aliases = {"mc", "myc"})
                public final class AliasedCommand {

                    @Subcommand("run")
                    @Execute
                    public void run(@Sender CommandSource source, @Arg("value") String value) {
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("example.AliasedCommandCommandAdapterFactory")
                .contentsAsUtf8String()
                .contains("CommandAdapterFactory<AliasedCommand>");
    }

    @Test
    void generatedAdapterUsesCorrectBrigadierTypes() {
        JavaFileObject command = JavaFileObjects.forSourceString("example.MultiArg", """
                package example;

                import com.hanielfialho.api.annotation.Arg;
                import com.hanielfialho.api.annotation.Command;
                import com.hanielfialho.api.annotation.Execute;
                import com.hanielfialho.api.annotation.Sender;
                import com.hanielfialho.api.annotation.Subcommand;
                import com.hanielfialho.api.source.CommandSource;

                @Command(name = "multi")
                public final class MultiArg {

                    @Subcommand("add")
                    @Execute
                    public void add(@Sender CommandSource source, @Arg("x") int x, @Arg("y") int y, @Arg("name") String name) {
                    }
                }
                """);

        Compilation compilation =
                Compiler.javac().withProcessors(new CommandEngineProcessor()).compile(command);

        assertThat(compilation).succeeded();
        var adapter = assertThat(compilation)
                .generatedSourceFile("example.MultiArgCommandAdapter")
                .contentsAsUtf8String();
        adapter.contains("IntegerArgumentType");
        adapter.contains("StringArgumentType.getString(");
    }
}
