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
        if (generatedFactories.isEmpty()) {
            return;
        }

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
