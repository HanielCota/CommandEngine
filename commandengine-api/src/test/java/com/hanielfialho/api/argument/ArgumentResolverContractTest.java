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
package com.hanielfialho.api.argument;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class ArgumentResolverContractTest {

    private static final ArgumentResolver<Integer> RESOLVER = new ArgumentResolver<>() {
        @Override
        public Class<Integer> type() {
            return Integer.class;
        }

        @Override
        public Integer resolve(String input) {
            if (input == null || input.isBlank()) {
                return null;
            }
            try {
                return Integer.parseInt(input.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
    };

    @Test
    void resolverDoesNotAcceptIncompatibleType() {
        assertThat(RESOLVER.type()).isEqualTo(Integer.class);
    }

    @Test
    void resolverReturnsNullForInvalidInput() {
        assertThat(RESOLVER.resolve("not-a-number")).isNull();
    }

    @Test
    void resolverHandlesNullInput() {
        assertThat(RESOLVER.resolve(null)).isNull();
    }

    @Test
    void resolverHandlesEmptyInput() {
        assertThat(RESOLVER.resolve("")).isNull();
    }

    @Test
    void resolverPreservesReturnType() {
        Integer value = RESOLVER.resolve("42");
        assertThat(value).isInstanceOf(Integer.class);
        assertThat(value).isEqualTo(42);
    }
}
