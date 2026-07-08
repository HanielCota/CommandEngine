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
package com.hanielfialho.processor.model;

import java.util.Objects;
import org.jetbrains.annotations.Nullable;

public record ParameterModel(
        String name,
        String typeName,
        Kind kind,
        boolean optional,
        @Nullable String defaultValue,
        boolean greedy,
        @Nullable Double min,
        @Nullable Double max,
        int minLength,
        int maxLength,
        char shorthand,
        @Nullable String suggestionMethodName,
        boolean asyncSuggestions) {

    public ParameterModel {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(typeName, "typeName");
        Objects.requireNonNull(kind, "kind");
    }

    public String getName() {
        return name;
    }

    public String getTypeName() {
        return typeName;
    }

    public Kind getKind() {
        return kind;
    }

    public boolean isOptional() {
        return optional;
    }

    public @Nullable String getDefaultValue() {
        return defaultValue;
    }

    public boolean isGreedy() {
        return greedy;
    }

    public @Nullable Double getMin() {
        return min;
    }

    public @Nullable Double getMax() {
        return max;
    }

    public int getMinLength() {
        return minLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public char getShorthand() {
        return shorthand;
    }

    public @Nullable String getSuggestionMethodName() {
        return suggestionMethodName;
    }

    public boolean isAsyncSuggestions() {
        return asyncSuggestions;
    }

    public enum Kind {
        SENDER,
        ARGUMENT,
        FLAG
    }
}
