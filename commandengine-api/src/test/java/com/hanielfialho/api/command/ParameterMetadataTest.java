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

import org.junit.jupiter.api.Test;

final class ParameterMetadataTest {

    @Test
    void createsRequiredArgument() {
        var param = new ParameterMetadata("name", String.class, ParameterMetadata.ParameterKind.ARGUMENT, null, false);
        assertThat(param.name()).isEqualTo("name");
        assertThat(param.type()).isEqualTo(String.class);
        assertThat(param.kind()).isEqualTo(ParameterMetadata.ParameterKind.ARGUMENT);
        assertThat(param.defaultValue()).isNull();
        assertThat(param.optional()).isFalse();
    }

    @Test
    void createsOptionalArgumentWithDefault() {
        var param = new ParameterMetadata("count", int.class, ParameterMetadata.ParameterKind.ARGUMENT, "0", true);
        assertThat(param.optional()).isTrue();
        assertThat(param.defaultValue()).isEqualTo("0");
    }

    @Test
    void createsFlagParameter() {
        var param = new ParameterMetadata("admin", boolean.class, ParameterMetadata.ParameterKind.FLAG, null, false);
        assertThat(param.kind()).isEqualTo(ParameterMetadata.ParameterKind.FLAG);
    }

    @Test
    void createsSenderParameter() {
        var param = new ParameterMetadata("sender", Object.class, ParameterMetadata.ParameterKind.SENDER, null, false);
        assertThat(param.kind()).isEqualTo(ParameterMetadata.ParameterKind.SENDER);
    }

    @Test
    void throwsOnNullName() {
        assertThatThrownBy(() -> new ParameterMetadata(
                        null, String.class, ParameterMetadata.ParameterKind.ARGUMENT, null, false))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("name");
    }

    @Test
    void throwsOnNullType() {
        assertThatThrownBy(
                        () -> new ParameterMetadata("arg", null, ParameterMetadata.ParameterKind.ARGUMENT, null, false))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("type");
    }

    @Test
    void throwsOnNullKind() {
        assertThatThrownBy(() -> new ParameterMetadata("arg", String.class, null, null, false))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("kind");
    }

    @Test
    void throwsWhenDefaultValueProvidedButNotOptional() {
        assertThatThrownBy(() -> new ParameterMetadata(
                        "arg", String.class, ParameterMetadata.ParameterKind.ARGUMENT, "default", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("defaultValue requires optional=true");
    }

    @Test
    void allowsDefaultValueWhenOptional() {
        var param =
                new ParameterMetadata("arg", String.class, ParameterMetadata.ParameterKind.ARGUMENT, "default", true);
        assertThat(param.defaultValue()).isEqualTo("default");
    }

    @Test
    void throwsOnPrimitiveOptionalWithoutDefault() {
        assertThatThrownBy(() ->
                        new ParameterMetadata("count", int.class, ParameterMetadata.ParameterKind.ARGUMENT, null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("primitive optional parameter requires a non-blank defaultValue");
    }

    @Test
    void throwsOnPrimitiveOptionalWithBlankDefault() {
        assertThatThrownBy(() ->
                        new ParameterMetadata("count", int.class, ParameterMetadata.ParameterKind.ARGUMENT, "  ", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("primitive optional parameter requires a non-blank defaultValue");
    }

    @Test
    void allowsNonPrimitiveOptionalWithoutDefault() {
        var param = new ParameterMetadata("name", String.class, ParameterMetadata.ParameterKind.ARGUMENT, null, true);
        assertThat(param.optional()).isTrue();
        assertThat(param.defaultValue()).isNull();
    }
}
