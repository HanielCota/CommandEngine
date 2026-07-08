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
package com.hanielfialho.api.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.hanielfialho.api.command.CommandPath;
import com.hanielfialho.api.source.CommandSource;
import org.junit.jupiter.api.Test;

final class CommandEventTest {

    @Test
    void carriesSourceAndPath() {
        var path = new CommandPath("test");
        var source = new TestSource("sender");
        var event = new TestCommandEvent(source, path);

        assertThat(event.source()).isSameAs(source);
        assertThat(event.path()).isEqualTo(path);
    }

    private static final class TestCommandEvent implements CommandEvent {

        private final CommandSource source;
        private final CommandPath path;

        TestCommandEvent(CommandSource source, CommandPath path) {
            this.source = source;
            this.path = path;
        }

        @Override
        public CommandSource source() {
            return source;
        }

        @Override
        public CommandPath path() {
            return path;
        }
    }

    private static final class TestSource implements CommandSource {

        private final String name;

        TestSource(String name) {
            this.name = name;
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
        public void sendMessage(String message) {}

        @Override
        public String getName() {
            return name;
        }
    }
}
