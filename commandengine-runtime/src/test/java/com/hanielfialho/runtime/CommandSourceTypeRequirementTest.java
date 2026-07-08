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
package com.hanielfialho.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.hanielfialho.api.source.CommandSource;
import java.util.Objects;
import org.junit.jupiter.api.Test;

final class CommandSourceTypeRequirementTest {

    private enum SenderType {
        PLAYER,
        CONSOLE,
        CUSTOM
    }

    @Test
    void onlyPlayer() {
        var player = new TypedSource(SenderType.PLAYER, "player1");
        var console = new TypedSource(SenderType.CONSOLE, "Console");

        assertThat(checkRequirement(player, SenderType.PLAYER)).isTrue();
        assertThat(checkRequirement(console, SenderType.PLAYER)).isFalse();
    }

    @Test
    void onlyConsole() {
        var console = new TypedSource(SenderType.CONSOLE, "Console");
        var player = new TypedSource(SenderType.PLAYER, "player1");

        assertThat(checkRequirement(console, SenderType.CONSOLE)).isTrue();
        assertThat(checkRequirement(player, SenderType.CONSOLE)).isFalse();
    }

    @Test
    void anySender() {
        var player = new TypedSource(SenderType.PLAYER, "player1");
        var console = new TypedSource(SenderType.CONSOLE, "Console");
        var custom = new TypedSource(SenderType.CUSTOM, "custom");

        assertThat(checkRequirement(player, null)).isTrue();
        assertThat(checkRequirement(console, null)).isTrue();
        assertThat(checkRequirement(custom, null)).isTrue();
    }

    @Test
    void customSender() {
        var custom = new TypedSource(SenderType.CUSTOM, "myBot");
        assertThat(checkRequirement(custom, SenderType.CUSTOM)).isTrue();
    }

    @Test
    void errorWhenSenderDoesNotMeetRequirement() {
        var player = new TypedSource(SenderType.PLAYER, "player1");
        assertThat(checkRequirement(player, SenderType.CONSOLE)).isFalse();
    }

    private static boolean checkRequirement(CommandSource source, SenderType required) {
        if (required == null) {
            return true;
        }
        if (source instanceof TypedSource typed) {
            return typed.type() == required;
        }
        return false;
    }

    private record TypedSource(SenderType type, String name) implements CommandSource {

        TypedSource {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(name, "name");
        }

        @Override
        public boolean hasPermission(String permission) {
            return true;
        }

        @Override
        public Object getHandle() {
            return this;
        }

        @Override
        public void sendMessage(String message) {
            // no-op: test source
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
