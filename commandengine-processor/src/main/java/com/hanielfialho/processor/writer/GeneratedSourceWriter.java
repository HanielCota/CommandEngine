package com.hanielfialho.processor.writer;

import java.io.IOException;
import java.io.Writer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

public final class GeneratedSourceWriter {

    private final ProcessingEnvironment processingEnv;
    private final TypeElement originatingElement;

    public GeneratedSourceWriter(ProcessingEnvironment processingEnv, TypeElement originatingElement) {
        this.processingEnv = processingEnv;
        this.originatingElement = originatingElement;
    }

    public void write(String qualifiedName, String source) throws IOException {
        var file = processingEnv.getFiler().createSourceFile(qualifiedName, originatingElement);
        try (Writer writer = file.openWriter()) {
            writer.write(source);
        }
    }
}
