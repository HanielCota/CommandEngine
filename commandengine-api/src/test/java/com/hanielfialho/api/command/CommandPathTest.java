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

final class CommandPathTest {

    @Test
    void createsPathFromVarargs() {
        var path = new CommandPath("guild", "create");
        assertThat(path.parts()).containsExactly("guild", "create");
    }

    @Test
    void createsPathFromList() {
        var path = new CommandPath(List.of("root"));
        assertThat(path.parts()).containsExactly("root");
    }

    @Test
    void emptyReturnsPathWithEmptyRoot() {
        var path = CommandPath.empty();
        assertThat(path.parts()).containsExactly("");
        assertThat(path.root()).isEmpty();
    }

    @Test
    void rootReturnsFirstPart() {
        var path = new CommandPath("guild", "invite");
        assertThat(path.root()).isEqualTo("guild");
    }

    @Test
    void toStringJoinsParts() {
        var path = new CommandPath("guild", "invite", "accept");
        assertThat(path).hasToString("guild invite accept");
    }

    @Test
    void singlePartToString() {
        var path = new CommandPath("help");
        assertThat(path).hasToString("help");
    }

    @Test
    void throwsOnNullVarargs() {
        assertThatThrownBy(() -> new CommandPath((String[]) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("parts");
    }

    @Test
    void throwsOnNullList() {
        assertThatThrownBy(() -> new CommandPath((List<String>) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("parts");
    }

    @Test
    void throwsOnEmptyPartsList() {
        assertThatThrownBy(() -> new CommandPath(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("parts must not be empty");
    }

    @Test
    void protectsImmutabilityOfPartsList() {
        var original = new java.util.ArrayList<>(List.of("cmd", "sub"));
        var path = new CommandPath(original);
        original.add("injected");
        assertThat(path.parts()).containsExactly("cmd", "sub");
    }
}
