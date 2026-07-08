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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class CommandMetadataBuilderTest {

    @Test
    void metadataIsImmutable() {
        var aliases = new ArrayList<>(List.of("a"));
        var cmd = new CommandMetadata("test", aliases, "desc", "perm", List.of());
        aliases.add("injected");
        assertThat(cmd.aliases()).containsExactly("a");
    }

    @Test
    void metadataMergeNotApplicable() {
        var cmd = new CommandMetadata("test", List.of(), "desc", "perm", List.of());
        assertThat(cmd.name()).isEqualTo("test");
        assertThat(cmd.description()).isEqualTo("desc");
        assertThat(cmd.permission()).isEqualTo("perm");
        assertThat(cmd.aliases()).isEmpty();
        assertThat(cmd.subcommands()).isEmpty();
    }

    @Test
    void keyOverwriteNotApplicable() {
        var cmd = new CommandMetadata("test", List.of("t"), "desc", "perm", List.of());
        assertThat(cmd.name()).isEqualTo("test");
        assertThat(cmd.aliases()).containsExactly("t");
    }

    @Test
    void keyInexistentNotApplicable() {
        assertThatThrownBy(() -> new CommandMetadata(null, List.of(), "desc", "perm", List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("name");

        assertThatThrownBy(() -> new CommandMetadata("", List.of(), "desc", "perm", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name must not be blank");
    }
}
