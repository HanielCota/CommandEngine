package com.hanielfialho.processor.generator;

import com.hanielfialho.processor.metadata.AdapterMetadataRenderer;
import com.hanielfialho.processor.model.CommandModel;
import com.hanielfialho.processor.writer.GeneratedSourceWriter;
import java.io.IOException;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

public final class AdapterGenerator {

    private final ProcessingEnvironment processingEnv;
    private final CommandModel model;
    private final TypeElement element;

    public AdapterGenerator(ProcessingEnvironment processingEnv, CommandModel model, TypeElement element) {
        this.processingEnv = processingEnv;
        this.model = model;
        this.element = element;
    }

    public void generate() throws IOException {
        String packageName = model.getPackageName();
        String adapterName = model.getAdapterClassName();
        String factoryName = adapterName + "Factory";
        var writer = new GeneratedSourceWriter(processingEnv, element);

        writer.write(qualifiedName(packageName, adapterName), render(packageName, adapterName));
        writer.write(
                qualifiedName(packageName, factoryName),
                new AdapterFactoryRenderer(model).render(packageName, adapterName, factoryName));
    }

    private String render(String packageName, String adapterName) {
        StringBuilder code = new StringBuilder(16_384);
        new AdapterImportsRenderer().render(code, packageName);
        new AdapterMemberRenderer(model).render(code, adapterName);
        new AdapterTreeRenderer(model).render(code);
        new AdapterExecutionRenderer(model).render(code);
        new AdapterHelperRenderer(model).render(code);
        new AdapterMetadataRenderer(model).render(code);
        code.append("}\n");
        return code.toString();
    }

    private String qualifiedName(String packageName, String simpleName) {
        return packageName.isEmpty() ? simpleName : packageName + "." + simpleName;
    }
}
