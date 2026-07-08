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

final class CommandAliasTest {

    @Test
    void commandWithSingleAlias() {
        var cmd = new CommandMetadata("test", List.of("t"), "desc", "perm", List.of());
        assertThat(cmd.aliases()).containsExactly("t");
    }

    @Test
    void commandWithMultipleAliases() {
        var cmd = new CommandMetadata("test", List.of("t", "ts", "testing"), "desc", "perm", List.of());
        assertThat(cmd.aliases()).hasSize(3);
        assertThat(cmd.aliases()).containsExactly("t", "ts", "testing");
    }

    @Test
    void duplicateAliasIsAllowed() {
        var cmd = new CommandMetadata("test", List.of("t", "t"), "desc", "perm", List.of());
        assertThat(cmd.aliases()).containsExactly("t", "t");
    }

    @Test
    void aliasEqualToMainNameIsAllowed() {
        var cmd = new CommandMetadata("test", List.of("test", "alias"), "desc", "perm", List.of());
        assertThat(cmd.aliases()).containsExactly("test", "alias");
    }

    @Test
    void invalidAliasThrows() {
        assertThatThrownBy(() -> new CommandMetadata("test", List.of((String) null), "desc", "perm", List.of()))
                .isInstanceOf(NullPointerException.class);
    }
}
