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
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class PermissionInheritanceTest {

    @Test
    void rootPermissionInheritedBySubcommand() {
        var source = new TestSource("commandengine.command");
        assertThat(source.hasPermission("commandengine.command")).isTrue();
        assertThat(source.hasPermission("commandengine.command.sub")).isTrue();
    }

    @Test
    void subcommandOverridesPermission() {
        var source = new TestSource("commandengine.sub");
        assertThat(source.hasPermission("commandengine.sub")).isTrue();
        assertThat(source.hasPermission("commandengine.command")).isFalse();
    }

    @Test
    void subcommandWithoutPermissionUsesFallback() {
        var source = new TestSource("commandengine.command");
        assertThat(source.hasPermission("")).isTrue();
    }

    @Test
    void aliasRespectsSamePermission() {
        var source = new TestSource("commandengine.team");
        assertThat(source.hasPermission("commandengine.team")).isTrue();
        assertThat(source.hasPermission("commandengine.team.list")).isTrue();
    }

    private static final class TestSource implements CommandSource {

        private final Set<String> permissions;

        private TestSource(String... perms) {
            permissions = new LinkedHashSet<>(Set.of(perms));
        }

        @Override
        public boolean hasPermission(String permission) {
            if (permission.isEmpty()) {
                return true;
            }
            if (permissions.contains(permission)) {
                return true;
            }
            for (String perm : permissions) {
                if (permission.startsWith(perm + ".") || perm.startsWith(permission + ".")) {
                    return true;
                }
            }
            return false;
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
            return "test";
        }
    }
}
