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

import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * User-facing messages emitted by generated adapters and platform bridges.
 */
public record CommandMessages(
        @NotNull String internalError,
        @NotNull String invalidSender,
        @NotNull String invalidSyntax,
        @NotNull String noPermission,
        @NotNull String rateLimited) {

    public CommandMessages(
            @NotNull String internalError,
            @NotNull String invalidSender,
            @NotNull String invalidSyntax,
            @NotNull String noPermission) {
        this(internalError, invalidSender, invalidSyntax, noPermission, "You are executing this command too quickly.");
    }

    public CommandMessages {
        Objects.requireNonNull(internalError, "internalError");
        Objects.requireNonNull(invalidSender, "invalidSender");
        Objects.requireNonNull(invalidSyntax, "invalidSyntax");
        Objects.requireNonNull(noPermission, "noPermission");
        Objects.requireNonNull(rateLimited, "rateLimited");
    }

    public static @NotNull CommandMessages defaults() {
        return new CommandMessages(
                "An internal error occurred while executing this command.",
                "This command cannot be executed by this sender.",
                "Invalid command syntax.",
                "You do not have permission to execute this command.",
                "You are executing this command too quickly.");
    }
}
