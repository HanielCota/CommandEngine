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
package com.hanielfialho.api.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

final class CommandUsageTest {

    @Test
    void toStringRepresentsUsage() {
        var path = new CommandPath("guild", "invite");
        assertThat(path).hasToString("guild invite");
    }

    @Test
    void customUsageNotApplicable() {
        var cmd = new CommandMetadata("test", List.of(), "desc", "perm", List.of());
        assertThat(cmd.name()).isEqualTo("test");
        assertThat(cmd.description()).isEqualTo("desc");
    }

    @Test
    void usageWithSubcommands() {
        var sub = new SubcommandMetadata("reload", "admin.reload", "Reload config", false, List.of());
        var cmd = new CommandMetadata("test", List.of(), "desc", "perm", List.of(sub));
        assertThat(cmd.subcommands()).hasSize(1);
        assertThat(cmd.subcommands().get(0).path()).isEqualTo("reload");
    }

    @Test
    void usageWithOptionalArguments() {
        var param = new ParameterMetadata("target", String.class, ParameterMetadata.ParameterKind.ARGUMENT, null, true);
        assertThat(param.optional()).isTrue();
        assertThat(param.defaultValue()).isNull();
    }
}
