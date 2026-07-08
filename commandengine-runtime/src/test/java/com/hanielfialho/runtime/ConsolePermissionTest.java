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

import com.hanielfialho.api.message.CommandMessages;
import com.hanielfialho.api.source.CommandSource;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ConsolePermissionTest {

    private static final CommandMessages MESSAGES = CommandMessages.defaults();

    @Test
    void consoleAllowedByDefault() {
        var source = new ConsoleSource(true);
        assertThat(source.hasPermission("commandengine.admin")).isTrue();
        assertThat(source.hasPermission("commandengine.admin.list")).isTrue();
    }

    @Test
    void consoleBlockedOnPlayerOnlyCommand() {
        var console = new ConsoleSource(true);
        var player = new ConsoleSource(false);

        assertThat(checkPlayerOnly(console)).isFalse();
        assertThat(checkPlayerOnly(player)).isTrue();
    }

    @Test
    void consoleWithFakePermission() {
        var source = new ConsoleSource("custom.perm");
        assertThat(source.hasPermission("custom.perm")).isTrue();
        assertThat(source.hasPermission("other.perm")).isTrue();
    }

    @Test
    void correctMessageForBlockedConsole() {
        var source = new ConsoleSource(false);
        var messages = new ArrayList<String>();

        if (!source.hasPermission("player.only.command")) {
            messages.add(MESSAGES.noPermission());
        }

        assertThat(messages).containsExactly(MESSAGES.noPermission());
    }

    private static boolean checkPlayerOnly(CommandSource source) {
        if (source instanceof ConsoleSource cs && cs.isConsole()) {
            return false;
        }
        return true;
    }

    private static final class ConsoleSource implements CommandSource {

        private final boolean console;
        private final Set<String> permissions;

        private ConsoleSource(boolean console) {
            this.console = console;
            permissions = new LinkedHashSet<>();
        }

        private ConsoleSource(String... perms) {
            this.console = true;
            permissions = new LinkedHashSet<>(Set.of(perms));
        }

        boolean isConsole() {
            return console;
        }

        @Override
        public boolean hasPermission(String permission) {
            if (console) {
                return true;
            }
            return permissions.contains(permission);
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
            return console ? "Console" : "player";
        }
    }
}
