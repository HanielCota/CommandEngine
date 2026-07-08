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

import com.hanielfialho.api.source.CommandSource;
import java.util.List;
import org.junit.jupiter.api.Test;

final class CommandPermissionTest {

    @Test
    void simplePermissionIsStoredInMetadata() {
        var metadata = new CommandMetadata("test", List.of(), "desc", "test.use", List.of());
        assertThat(metadata.permission()).isEqualTo("test.use");
    }

    @Test
    void emptyPermissionIsAllowed() {
        var metadata = new CommandMetadata("test", List.of(), "desc", "", List.of());
        assertThat(metadata.permission()).isEmpty();
    }

    @Test
    void sourceWithPermissionIsAllowed() {
        var source = new TestSource("player");
        source.addPermission("test.use");
        assertThat(source.hasPermission("test.use")).isTrue();
    }

    @Test
    void sourceWithoutPermissionIsDenied() {
        var source = new TestSource("player");
        assertThat(source.hasPermission("admin.use")).isFalse();
    }

    @Test
    void emptyPermissionAlwaysPasses() {
        var source = new TestSource("player");
        assertThat(source.hasPermission("")).isTrue();
    }

    @Test
    void subcommandMetadataStoresPermission() {
        var subcommand = new SubcommandMetadata("sub", "sub.use", "sub desc", false, List.of());
        assertThat(subcommand.permission()).isEqualTo("sub.use");
    }

    private static final class TestSource implements CommandSource {

        private final String name;
        private final java.util.Set<String> permissions = new java.util.HashSet<>();

        TestSource(String name) {
            this.name = name;
        }

        void addPermission(String permission) {
            permissions.add(permission);
        }

        @Override
        public boolean hasPermission(String permission) {
            return permission.isEmpty() || permissions.contains(permission);
        }

        @Override
        public Object getHandle() {
            return this;
        }

        @Override
        public void sendMessage(String message) {
            // no-op: test stub
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
