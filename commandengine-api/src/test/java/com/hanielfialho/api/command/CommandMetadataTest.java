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

import java.util.List;
import org.junit.jupiter.api.Test;

final class CommandMetadataTest {

    @Test
    void createsMetadataWithValidArguments() {
        var meta = new CommandMetadata("test", List.of("t"), "desc", "perm", List.of());
        assertThat(meta.name()).isEqualTo("test");
        assertThat(meta.aliases()).containsExactly("t");
        assertThat(meta.description()).isEqualTo("desc");
        assertThat(meta.permission()).isEqualTo("perm");
        assertThat(meta.subcommands()).isEmpty();
    }

    @Test
    void throwsOnNullName() {
        assertThatThrownBy(() -> new CommandMetadata(null, List.of(), "", "", List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("name");
    }

    @Test
    void throwsOnBlankName() {
        assertThatThrownBy(() -> new CommandMetadata("  ", List.of(), "", "", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name must not be blank");
    }

    @Test
    void throwsOnNullAliases() {
        assertThatThrownBy(() -> new CommandMetadata("test", null, "", "", List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("aliases");
    }

    @Test
    void throwsOnNullDescription() {
        assertThatThrownBy(() -> new CommandMetadata("test", List.of(), null, "", List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("description");
    }

    @Test
    void throwsOnNullPermission() {
        assertThatThrownBy(() -> new CommandMetadata("test", List.of(), "", null, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("permission");
    }

    @Test
    void throwsOnNullSubcommands() {
        assertThatThrownBy(() -> new CommandMetadata("test", List.of(), "", "", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("subcommands");
    }

    @Test
    void protectsImmutabilityOfAliases() {
        var mutable = new java.util.ArrayList<>(List.of("a"));
        var meta = new CommandMetadata("test", mutable, "", "", List.of());
        mutable.add("injected");
        assertThat(meta.aliases()).containsExactly("a");
    }

    @Test
    void protectsImmutabilityOfSubcommands() {
        var sub = new SubcommandMetadata("sub", "", "", false, List.of());
        var mutable = new java.util.ArrayList<>(List.of(sub));
        var meta = new CommandMetadata("test", List.of(), "", "", mutable);
        mutable.clear();
        assertThat(meta.subcommands()).hasSize(1);
    }
}
