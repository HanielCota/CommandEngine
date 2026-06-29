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
