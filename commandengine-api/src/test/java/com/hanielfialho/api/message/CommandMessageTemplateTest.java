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
package com.hanielfialho.api.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

final class CommandMessageTemplateTest {

    @Test
    void simplePlaceholder() {
        var msgs = new CommandMessages("Error: {reason}", "sender", "syntax", "noperm", "rate");
        assertThat(msgs.internalError()).isEqualTo("Error: {reason}");
    }

    @Test
    void placeholderAbsent() {
        var msgs = new CommandMessages("Plain error", "sender", "syntax", "noperm", "rate");
        assertThat(msgs.internalError()).doesNotContain("{");
    }

    @Test
    void placeholderRepeated() {
        var msgs = new CommandMessages("Error {x} occurred at {x}", "sender", "syntax", "noperm", "rate");
        assertThat(msgs.internalError()).isEqualTo("Error {x} occurred at {x}");
    }

    @Test
    void nullValue() {
        assertThatThrownBy(() -> new CommandMessages("err", null, "syntax", "noperm", "rate"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("invalidSender");
    }

    @Test
    void escaping() {
        var msgs = new CommandMessages("Error: <test> & special \"chars\"", "sender", "syntax", "noperm", "rate");
        assertThat(msgs.internalError()).isEqualTo("Error: <test> & special \"chars\"");
    }
}
