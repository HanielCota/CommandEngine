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

final class ArgumentDefinitionTest {

    @Test
    void requiredArgument() {
        var param =
                new ParameterMetadata("target", String.class, ParameterMetadata.ParameterKind.ARGUMENT, null, false);
        assertThat(param.name()).isEqualTo("target");
        assertThat(param.type()).isEqualTo(String.class);
        assertThat(param.kind()).isEqualTo(ParameterMetadata.ParameterKind.ARGUMENT);
        assertThat(param.defaultValue()).isNull();
        assertThat(param.optional()).isFalse();
    }

    @Test
    void optionalArgument() {
        var param = new ParameterMetadata("flag", Boolean.class, ParameterMetadata.ParameterKind.ARGUMENT, null, true);
        assertThat(param.optional()).isTrue();
        assertThat(param.defaultValue()).isNull();
    }

    @Test
    void argumentWithDefaultValue() {
        var param = new ParameterMetadata("count", Integer.class, ParameterMetadata.ParameterKind.ARGUMENT, "10", true);
        assertThat(param.defaultValue()).isEqualTo("10");
        assertThat(param.optional()).isTrue();
    }

    @Test
    void variadicLikeArgument() {
        var params = List.of(
                new ParameterMetadata("player", String.class, ParameterMetadata.ParameterKind.ARGUMENT, null, false),
                new ParameterMetadata("reason", String.class, ParameterMetadata.ParameterKind.ARGUMENT, null, true));
        assertThat(params).hasSize(2);
        assertThat(params.get(0).name()).isEqualTo("player");
        assertThat(params.get(0).optional()).isFalse();
        assertThat(params.get(1).name()).isEqualTo("reason");
        assertThat(params.get(1).optional()).isTrue();
    }

    @Test
    void invalidName() {
        assertThatThrownBy(() -> new ParameterMetadata(
                        null, String.class, ParameterMetadata.ParameterKind.ARGUMENT, null, false))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("name");
    }
}
