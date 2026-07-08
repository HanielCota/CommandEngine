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
