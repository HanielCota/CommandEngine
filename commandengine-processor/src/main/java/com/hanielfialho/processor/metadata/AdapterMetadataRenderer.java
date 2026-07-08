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
package com.hanielfialho.processor.metadata;

import com.hanielfialho.processor.model.CommandModel;
import com.hanielfialho.processor.model.SubcommandModel;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class AdapterMetadataRenderer {

    private final CommandModel model;

    public AdapterMetadataRenderer(CommandModel model) {
        this.model = model;
    }

    public void render(StringBuilder code) {
        code.append("    @Override\n");
        code.append("    public CommandMetadata metadata() {\n");
        code.append("        return new CommandMetadata(\n");
        code.append("            \"").append(escape(model.getName())).append("\",\n");
        code.append("            ")
                .append(listOfStrings(List.of(model.getAliases())))
                .append(",\n");
        code.append("            \"").append(escape(model.getDescription())).append("\",\n");
        code.append("            \"").append(escape(model.getPermission())).append("\",\n");
        code.append("            List.of(\n");
        for (int i = 0; i < model.getSubcommands().size(); i++) {
            SubcommandModel subcommand = model.getSubcommands().get(i);
            code.append("                new SubcommandMetadata(\"")
                    .append(escape(subcommand.getPath()))
                    .append("\", \"")
                    .append(escape(subcommand.getPermission()))
                    .append("\", \"")
                    .append(escape(subcommand.getDescription()))
                    .append("\", ")
                    .append(subcommand.isAsync())
                    .append(", ")
                    .append(parameterMetadata(subcommand))
                    .append(")");
            if (i < model.getSubcommands().size() - 1) {
                code.append(",");
            }
            code.append("\n");
        }
        code.append("            )\n");
        code.append("        );\n");
        code.append("    }\n");
    }

    private String parameterMetadata(SubcommandModel subcommand) {
        if (subcommand.getParameters().isEmpty()) {
            return "List.of()";
        }

        return subcommand.getParameters().stream()
                .map(parameter -> "new ParameterMetadata(\""
                        + escape(parameter.getName())
                        + "\", "
                        + classLiteral(parameter.getTypeName())
                        + ", ParameterMetadata.ParameterKind."
                        + parameter.getKind().name()
                        + ", "
                        + (parameter.getDefaultValue() == null
                                ? "null"
                                : "\"" + escape(parameter.getDefaultValue()) + "\"")
                        + ", "
                        + parameter.isOptional()
                        + ")")
                .collect(Collectors.joining(", ", "List.of(", ")"));
    }

    private String listOfStrings(List<String> values) {
        if (values.isEmpty()) {
            return "List.of()";
        }
        return values.stream()
                .map(value -> "\"" + escape(value) + "\"")
                .collect(Collectors.joining(", ", "List.of(", ")"));
    }

    private String classLiteral(String typeName) {
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

    private String rawTypeName(String typeName) {
        int genericStart = typeName.indexOf('<');
        return genericStart < 0 ? typeName : typeName.substring(0, genericStart);
    }

    private String escape(String value) {
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

    private void appendEscapedDefault(StringBuilder escaped, char current) {
        if (Character.isISOControl(current)) {
            escaped.append(String.format(Locale.ROOT, "\\u%04x", (int) current));
            return;
        }

        escaped.append(current);
    }
}
