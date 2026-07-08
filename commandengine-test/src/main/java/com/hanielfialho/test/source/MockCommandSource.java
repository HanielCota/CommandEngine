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
package com.hanielfialho.test.source;

import com.hanielfialho.api.source.CommandSource;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jetbrains.annotations.NotNull;

public class MockCommandSource implements CommandSource {

    private final String name;
    private final Set<String> permissions = new HashSet<>();
    private final List<String> messages = new CopyOnWriteArrayList<>();

    public MockCommandSource(@NotNull String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public void addPermission(@NotNull String permission) {
        permissions.add(Objects.requireNonNull(permission, "permission"));
    }

    public @NotNull List<String> messages() {
        return List.copyOf(messages);
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        Objects.requireNonNull(permission, "permission");
        return permission.isEmpty() || permissions.contains(permission);
    }

    @Override
    public @NotNull Object getHandle() {
        return this;
    }

    @Override
    public void sendMessage(@NotNull String message) {
        messages.add(Objects.requireNonNull(message, "message"));
    }

    @Override
    public @NotNull String getName() {
        return name;
    }
}
