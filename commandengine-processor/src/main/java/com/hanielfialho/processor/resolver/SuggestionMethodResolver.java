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
package com.hanielfialho.processor.resolver;

import com.hanielfialho.api.annotation.SuggestionProvider;
import com.hanielfialho.processor.model.SuggestionMethodModel;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;

public final class SuggestionMethodResolver {

    private final ProcessingEnvironment processingEnv;

    public SuggestionMethodResolver(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    public Map<String, SuggestionMethodModel> resolve(TypeElement typeElement) {
        var methods = new HashMap<String, SuggestionMethodModel>();
        var current = typeElement;
        while (current != null) {
            for (var enclosed : current.getEnclosedElements()) {
                resolveMethod(enclosed, methods);
            }
            var superType = current.getSuperclass();
            TypeElement next = null;
            if (superType != null && !superType.toString().equals("java.lang.Object")) {
                var superElement = (TypeElement) processingEnv.getTypeUtils().asElement(superType);
                if (superElement != null && !superElement.equals(current)) {
                    next = superElement;
                }
            }
            current = next;
        }
        return Map.copyOf(methods);
    }

    @SuppressWarnings("java:S2201")
    private void resolveMethod(Element enclosed, Map<String, SuggestionMethodModel> methods) {
        if (enclosed.getKind() != ElementKind.METHOD) {
            return;
        }

        var annotation = enclosed.getAnnotation(SuggestionProvider.class);
        if (annotation == null) {
            return;
        }

        var method = (ExecutableElement) enclosed;
        if (annotation.value().isBlank()) {
            processingEnv
                    .getMessager()
                    .printMessage(Diagnostic.Kind.ERROR, "@SuggestionProvider value must not be blank", enclosed);
            return;
        }
        if (methods.containsKey(annotation.value())) {
            processingEnv
                    .getMessager()
                    .printMessage(
                            Diagnostic.Kind.ERROR,
                            "Duplicate @SuggestionProvider value: " + annotation.value(),
                            enclosed);
            return;
        }

        if (!method.getParameters().isEmpty()) {
            processingEnv
                    .getMessager()
                    .printMessage(
                            Diagnostic.Kind.ERROR, "@SuggestionProvider methods must not declare parameters", enclosed);
            return;
        }

        if (method.getModifiers().contains(Modifier.PRIVATE)
                || method.getModifiers().contains(Modifier.STATIC)) {
            processingEnv
                    .getMessager()
                    .printMessage(
                            Diagnostic.Kind.ERROR,
                            "@SuggestionProvider methods must be instance methods accessible to the generated adapter",
                            enclosed);
            return;
        }

        if (!returnsListOfString(method)) {
            processingEnv
                    .getMessager()
                    .printMessage(
                            Diagnostic.Kind.ERROR,
                            "@SuggestionProvider methods must return java.util.List<String>",
                            enclosed);
            return;
        }

        if (hasCheckedExceptions(method)) {
            processingEnv
                    .getMessager()
                    .printMessage(
                            Diagnostic.Kind.ERROR,
                            "@SuggestionProvider methods must not declare checked exceptions",
                            enclosed);
            return;
        }

        methods.put(
                annotation.value(),
                new SuggestionMethodModel(method.getSimpleName().toString(), annotation.async()));
    }

    private boolean hasCheckedExceptions(ExecutableElement method) {
        var runtimeException = processingEnv.getElementUtils().getTypeElement("java.lang.RuntimeException");
        var error = processingEnv.getElementUtils().getTypeElement("java.lang.Error");
        if (runtimeException == null || error == null) {
            return !method.getThrownTypes().isEmpty();
        }
        var runtimeType = runtimeException.asType();
        var errorType = error.asType();
        var typeUtils = processingEnv.getTypeUtils();
        for (var thrownType : method.getThrownTypes()) {
            if (!typeUtils.isSubtype(thrownType, runtimeType) && !typeUtils.isSubtype(thrownType, errorType)) {
                return true;
            }
        }
        return false;
    }

    private boolean returnsListOfString(ExecutableElement method) {
        var returnType = method.getReturnType();
        if (returnType.getKind() != TypeKind.DECLARED) {
            return false;
        }
        var declared = (DeclaredType) returnType;
        var element = (TypeElement) declared.asElement();
        if (!element.getQualifiedName().contentEquals("java.util.List")) {
            return false;
        }
        var typeArguments = declared.getTypeArguments();
        if (typeArguments.size() != 1) {
            return false;
        }
        var stringElement = processingEnv.getElementUtils().getTypeElement("java.lang.String");
        if (stringElement == null) {
            return false;
        }
        return processingEnv.getTypeUtils().isSameType(typeArguments.get(0), stringElement.asType());
    }
}
