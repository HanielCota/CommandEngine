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

final class SubcommandMetadataTest {

    @Test
    void createsSubcommandWithValidArguments() {
        var params = List.of(
                new ParameterMetadata("arg", String.class, ParameterMetadata.ParameterKind.ARGUMENT, null, false));
        var sub = new SubcommandMetadata("sub", "perm", "desc", true, params);
        assertThat(sub.path()).isEqualTo("sub");
        assertThat(sub.permission()).isEqualTo("perm");
        assertThat(sub.description()).isEqualTo("desc");
        assertThat(sub.async()).isTrue();
        assertThat(sub.parameters()).containsExactlyElementsOf(params);
    }

    @Test
    void throwsOnNullPath() {
        assertThatThrownBy(() -> new SubcommandMetadata(null, "", "", false, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("path");
    }

    @Test
    void throwsOnNullPermission() {
        assertThatThrownBy(() -> new SubcommandMetadata("sub", null, "", false, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("permission");
    }

    @Test
    void throwsOnNullDescription() {
        assertThatThrownBy(() -> new SubcommandMetadata("sub", "", null, false, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("description");
    }

    @Test
    void throwsOnNullParameters() {
        assertThatThrownBy(() -> new SubcommandMetadata("sub", "", "", false, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("parameters");
    }

    @Test
    void protectsImmutabilityOfParameters() {
        var mutable = new java.util.ArrayList<>(List.of(
                new ParameterMetadata("arg", String.class, ParameterMetadata.ParameterKind.ARGUMENT, null, false)));
        var sub = new SubcommandMetadata("sub", "", "", false, mutable);
        mutable.clear();
        assertThat(sub.parameters()).hasSize(1);
    }
}
