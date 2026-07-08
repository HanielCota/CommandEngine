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
package com.hanielfialho.processor;

import com.hanielfialho.api.annotation.Command;
import com.hanielfialho.processor.generator.AdapterGenerator;
import com.hanielfialho.processor.model.CommandDefinition;
import com.hanielfialho.processor.reader.CommandModelReader;
import com.hanielfialho.processor.writer.CommandAdapterServiceWriter;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes("com.hanielfialho.api.annotation.Command")
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public final class CommandEngineProcessor extends AbstractProcessor {

    private final Set<String> generatedFactories = new TreeSet<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            new CommandAdapterServiceWriter(processingEnv).write(generatedFactories);
            return false;
        }

        if (annotations.isEmpty()) {
            return false;
        }

        for (var element : roundEnv.getElementsAnnotatedWith(Command.class)) {
            new CommandModelReader(processingEnv).read(element).ifPresent(this::processCommand);
        }

        return true;
    }

    @SuppressWarnings("java:S2201")
    private void processCommand(CommandDefinition definition) {
        try {
            var model = definition.model();
            var generator = new AdapterGenerator(processingEnv, model, definition.element());
            generator.generate();
            var factoryName = model.getAdapterClassName() + "Factory";
            generatedFactories.add(
                    model.getPackageName().isEmpty() ? factoryName : model.getPackageName() + "." + factoryName);
        } catch (Exception e) {
            processingEnv
                    .getMessager()
                    .printMessage(
                            Diagnostic.Kind.ERROR,
                            "Failed to generate adapter: " + e.getMessage(),
                            definition.element());
        }
    }
}
