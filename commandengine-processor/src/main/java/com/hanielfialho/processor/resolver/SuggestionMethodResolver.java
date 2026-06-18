package com.hanielfialho.processor.resolver;

import com.hanielfialho.api.annotation.SuggestionProvider;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

public final class SuggestionMethodResolver {

    private final ProcessingEnvironment processingEnv;

    public SuggestionMethodResolver(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    public Map<String, String> resolve(TypeElement typeElement) {
        var methods = new HashMap<String, String>();
        for (var enclosed : typeElement.getEnclosedElements()) {
            resolveMethod(enclosed, methods);
        }
        return Map.copyOf(methods);
    }

    private void resolveMethod(Element enclosed, Map<String, String> methods) {
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

        if (!"java.util.List<java.lang.String>".equals(method.getReturnType().toString())) {
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

        methods.put(annotation.value(), method.getSimpleName().toString());
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
}
