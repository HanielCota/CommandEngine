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

final class CommandMessagesTest {

    @Test
    void defaultsCreatesPredefinedMessages() {
        var msgs = CommandMessages.defaults();
        assertThat(msgs.internalError()).isEqualTo("An internal error occurred while executing this command.");
        assertThat(msgs.invalidSender()).isEqualTo("This command cannot be executed by this sender.");
        assertThat(msgs.invalidSyntax()).isEqualTo("Invalid command syntax.");
        assertThat(msgs.noPermission()).isEqualTo("You do not have permission to execute this command.");
        assertThat(msgs.rateLimited()).isEqualTo("You are executing this command too quickly.");
    }

    @Test
    void fourParamConstructorDefaultsRateLimited() {
        var msgs = new CommandMessages("err", "sender", "syntax", "perm");
        assertThat(msgs.internalError()).isEqualTo("err");
        assertThat(msgs.invalidSender()).isEqualTo("sender");
        assertThat(msgs.invalidSyntax()).isEqualTo("syntax");
        assertThat(msgs.noPermission()).isEqualTo("perm");
        assertThat(msgs.rateLimited()).isEqualTo("You are executing this command too quickly.");
    }

    @Test
    void fiveParamConstructorSetsAllFields() {
        var msgs = new CommandMessages("e", "s", "x", "p", "r");
        assertThat(msgs.internalError()).isEqualTo("e");
        assertThat(msgs.invalidSender()).isEqualTo("s");
        assertThat(msgs.invalidSyntax()).isEqualTo("x");
        assertThat(msgs.noPermission()).isEqualTo("p");
        assertThat(msgs.rateLimited()).isEqualTo("r");
    }

    @Test
    void throwsOnNullInternalError() {
        assertThatThrownBy(() -> new CommandMessages(null, "s", "x", "p", "r"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("internalError");
    }

    @Test
    void throwsOnNullInvalidSender() {
        assertThatThrownBy(() -> new CommandMessages("e", null, "x", "p", "r"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("invalidSender");
    }

    @Test
    void throwsOnNullInvalidSyntax() {
        assertThatThrownBy(() -> new CommandMessages("e", "s", null, "p", "r"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("invalidSyntax");
    }

    @Test
    void throwsOnNullNoPermission() {
        assertThatThrownBy(() -> new CommandMessages("e", "s", "x", null, "r"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("noPermission");
    }

    @Test
    void throwsOnNullRateLimited() {
        assertThatThrownBy(() -> new CommandMessages("e", "s", "x", "p", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("rateLimited");
    }
}
