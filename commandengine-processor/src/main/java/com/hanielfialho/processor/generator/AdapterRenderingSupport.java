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
package com.hanielfialho.processor.generator;

import com.hanielfialho.processor.model.CommandModel;
import com.hanielfialho.processor.model.SubcommandModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

final class AdapterRenderingSupport {

    private AdapterRenderingSupport() {}

    static String commandPathArguments(CommandModel model, SubcommandModel subcommand) {
        List<String> parts = new ArrayList<>();
        parts.add(model.getName());
        if (!subcommand.getPath().isBlank()) {
            parts.addAll(List.of(subcommand.getPath().trim().split("\\s+")));
        }
        return parts.stream().map(part -> "\"" + escape(part) + "\"").collect(Collectors.joining(", "));
    }

    static String classLiteral(String typeName) {
        return switch (typeName) {
            case "boolean" -> "boolean.class";
            case "byte" -> "byte.class";
            case "short" -> "short.class";
            case "int" -> "int.class";
            case "long" -> "long.class";
            case "float" -> "float.class";
            case "double" -> "double.class";
            case "char" -> "char.class";
            default -> rawTypeName(typeName) + ".class";
        };
    }

    static String rawTypeName(String typeName) {
        int genericStart = typeName.indexOf('<');
        return genericStart < 0 ? typeName : typeName.substring(0, genericStart);
    }

    static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            switch (current) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> appendEscapedDefault(escaped, current);
            }
        }
        return escaped.toString();
    }

    private static void appendEscapedDefault(StringBuilder escaped, char current) {
        if (Character.isISOControl(current)) {
            escaped.append(String.format(Locale.ROOT, "\\u%04x", (int) current));
            return;
        }

        escaped.append(current);
    }

    static String escapeChar(char value) {
        return switch (value) {
            case '\0' -> "\\0";
            case '\'' -> "\\'";
            case '\\' -> "\\\\";
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            case '\t' -> "\\t";
            default -> Character.toString(value);
        };
    }
}
