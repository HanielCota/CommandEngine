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
package com.hanielfialho.api.source;

import org.jetbrains.annotations.NotNull;

/**
 * Platform-agnostic abstraction for a command source.
 */
public interface CommandSource extends PermissionHolder {

    /**
     * Returns whether this source has the given permission.
     */
    boolean hasPermission(@NotNull String permission);

    /**
     * Returns the native platform handle, for example a Bukkit {@code Player}.
     * Use this carefully and prefer abstractions when possible.
     */
    @NotNull
    Object getHandle();

    /**
     * Sends a plain text message to this source.
     */
    void sendMessage(@NotNull String message);

    /**
     * Returns this source's display or identifier name.
     */
    @NotNull
    String getName();
}
