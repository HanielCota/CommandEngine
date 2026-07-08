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
package com.hanielfialho.api.rate;

import com.hanielfialho.api.command.CommandPath;
import com.hanielfialho.api.source.CommandSource;
import org.jetbrains.annotations.NotNull;

/**
 * Controls whether a sender can execute a command path at a given moment.
 */
public interface CommandRateLimiter {

    /**
     * Allows every command execution.
     */
    CommandRateLimiter NONE = (source, path) -> true;

    /**
     * Attempts to reserve one execution slot for the given source and command path.
     *
     * @param source source requesting command execution
     * @param path command path being executed
     * @return {@code true} when execution is allowed
     */
    boolean tryAcquire(@NotNull CommandSource source, @NotNull CommandPath path);
}
