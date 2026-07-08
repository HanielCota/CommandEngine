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

import com.hanielfialho.api.command.CommandPath;
import com.hanielfialho.api.message.CommandMessages;
import com.hanielfialho.api.result.CommandResult;
import com.hanielfialho.api.source.CommandSource;
import com.hanielfialho.runtime.internal.executor.SyncExecutor;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class CommandSenderFeedbackTest {

    private final SyncExecutor executor = new SyncExecutor();
    private final CommandPath path = new CommandPath("test");

    @Test
    void successMessage() {
        var source = new RecordingSource();

        executor.executeSync(source, path, () -> source.sendMessage("Command executed successfully"));

        assertThat(source.messages()).containsExactly("Command executed successfully");
    }

    @Test
    void errorMessage() {
        var source = new RecordingSource();

        executor.executeSync(source, path, () -> {
            source.sendMessage("Something went wrong");
            throw new RuntimeException("fail");
        });

        assertThat(source.messages()).containsExactly("Something went wrong");
    }

    @Test
    void noPermissionMessage() {
        var source = new RecordingSource();
        var messages = new CommandMessages("err", "sender", "syntax", "You do not have access!", "rate");
        var customExecutor = new SyncExecutor(messages);

        customExecutor.executeSync(source, path, () -> {
            if (!source.hasPermission("command.test")) {
                source.sendMessage(messages.noPermission());
                return;
            }
        });

        assertThat(source.messages()).containsExactly("You do not have access!");
    }

    @Test
    void usageMessage() {
        var source = new RecordingSource();

        executor.executeSync(source, path, () -> source.sendMessage("Usage: /test <arg>"));

        assertThat(source.messages()).containsExactly("Usage: /test <arg>");
    }

    @Test
    void silentCommandSendsNoMessage() {
        var source = new RecordingSource();

        executor.executeSync(source, path, () -> {});

        assertThat(source.messages()).isEmpty();
    }

    @Test
    void internalErrorMessageIsSentOnException() {
        var source = new RecordingSource();
        var messages = CommandMessages.defaults();

        var result = executor.executeSync(source, path, () -> {
            throw new RuntimeException("unexpected error");
        });

        assertThat(result).isInstanceOf(CommandResult.Failure.class);
        assertThat(((CommandResult.Failure) result).message()).isEqualTo(messages.internalError());
    }

    private static final class RecordingSource implements CommandSource {

        private final List<String> messages = new ArrayList<>();

        @Override
        public boolean hasPermission(String permission) {
            return "admin".equals(permission);
        }

        @Override
        public Object getHandle() {
            return this;
        }

        @Override
        public void sendMessage(String message) {
            messages.add(message);
        }

        @Override
        public String getName() {
            return "recorder";
        }

        List<String> messages() {
            return List.copyOf(messages);
        }
    }
}
