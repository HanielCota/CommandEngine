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
package com.hanielfialho.processor.writer;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;

public final class CommandAdapterServiceWriter {

    private static final String SERVICE_FILE = "META-INF/services/com.hanielfialho.api.command.CommandAdapterFactory";

    private final ProcessingEnvironment processingEnv;

    public CommandAdapterServiceWriter(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    public void write(Set<String> generatedFactories) {
        try {
            var file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", SERVICE_FILE);
            try (Writer writer = file.openWriter()) {
                for (var factory : generatedFactories) {
                    writer.write(factory);
                    writer.write(System.lineSeparator());
                }
            }
        } catch (IOException exception) {
            processingEnv
                    .getMessager()
                    .printMessage(
                            Diagnostic.Kind.ERROR,
                            "Failed to write CommandAdapterFactory service file: " + exception.getMessage());
        }
    }
}
