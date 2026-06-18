package com.hanielfialho.processor.reader;

import com.hanielfialho.api.annotation.Arg;
import com.hanielfialho.api.annotation.Command;
import com.hanielfialho.api.annotation.Execute;
import com.hanielfialho.api.annotation.Flag;
import com.hanielfialho.api.annotation.Greedy;
import com.hanielfialho.api.annotation.Max;
import com.hanielfialho.api.annotation.Min;
import com.hanielfialho.api.annotation.Range;
import com.hanielfialho.api.annotation.Sender;
import com.hanielfialho.api.annotation.Subcommand;
import com.hanielfialho.api.annotation.Suggestions;
import com.hanielfialho.processor.model.CommandDefinition;
import com.hanielfialho.processor.model.CommandModel;
import com.hanielfialho.processor.model.ParameterModel;
import com.hanielfialho.processor.model.SubcommandModel;
import com.hanielfialho.processor.resolver.SuggestionMethodResolver;
import com.hanielfialho.processor.validation.SupportedCommandTypes;
import java.util.Map;
import java.util.Optional;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;

public final class CommandModelReader {

    private final ProcessingEnvironment processingEnv;

    public CommandModelReader(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    public Optional<CommandDefinition> read(Element element) {
        if (element.getKind() != ElementKind.CLASS) {
            error("@Command can only be applied to classes", element);
            return Optional.empty();
        }

        var annotation = element.getAnnotation(Command.class);
        if (annotation.name().isBlank()) {
            error("@Command name must not be blank", element);
            return Optional.empty();
        }

        var typeElement = (TypeElement) element;
        var model = new CommandModel(
                typeElement.getQualifiedName().toString(),
                annotation.name(),
                annotation.aliases(),
                annotation.description(),
                annotation.permission());
        var suggestionMethods = new SuggestionMethodResolver(processingEnv).resolve(typeElement);

        for (var enclosed : typeElement.getEnclosedElements()) {
            readSubcommand(enclosed, annotation.permission(), suggestionMethods).ifPresent(model::addSubcommand);
        }

        return Optional.of(new CommandDefinition(typeElement, model));
    }

    private Optional<SubcommandModel> readSubcommand(
            Element element, String commandPermission, Map<String, String> suggestionMethods) {
        if (element.getKind() != ElementKind.METHOD) {
            return Optional.empty();
        }

        var sub = element.getAnnotation(Subcommand.class);
        var conventionRootHandler = sub == null && element.getSimpleName().contentEquals("onCommand");
        if (sub == null && !conventionRootHandler) {
            return Optional.empty();
        }

        if (sub != null && sub.value().isBlank()) {
            error("@Subcommand path must not be blank", element);
            return Optional.empty();
        }

        var method = (ExecutableElement) element;
        var exec = element.getAnnotation(Execute.class);
        var methodSuggestions = element.getAnnotation(Suggestions.class);
        var subcommandPath = conventionRootHandler ? "" : sub.value();
        var subcommandPermission =
                conventionRootHandler || sub.permission().isBlank() ? commandPermission : sub.permission();
        var returnsVoid = method.getReturnType().getKind() == TypeKind.VOID;
        if (!returnsVoid && exec != null && exec.async()) {
            error("@Execute(async = true) requires a void command handler", element);
            return Optional.empty();
        }
        var subcommand = new SubcommandModel(
                element.getSimpleName().toString(),
                subcommandPath,
                subcommandPermission,
                conventionRootHandler ? "" : sub.description(),
                returnsVoid && (exec == null || exec.async()),
                returnsVoid);

        for (var parameter : method.getParameters()) {
            if (!readParameter(parameter, subcommand, suggestionMethods, methodSuggestions)) {
                return Optional.empty();
            }
        }
        if (!validateGreedyPosition(subcommand, element)) {
            return Optional.empty();
        }
        if (!validateGreedyFlags(subcommand, element)) {
            return Optional.empty();
        }
        return Optional.of(subcommand);
    }

    private boolean readParameter(
            VariableElement parameter,
            SubcommandModel subcommand,
            Map<String, String> suggestionMethods,
            Suggestions methodSuggestions) {
        var sender = parameter.getAnnotation(Sender.class);
        var arg = parameter.getAnnotation(Arg.class);
        var flag = parameter.getAnnotation(Flag.class);
        var optional = parameter.getAnnotation(com.hanielfialho.api.annotation.Optional.class);
        var greedy = parameter.getAnnotation(Greedy.class);
        var suggestions = parameter.getAnnotation(Suggestions.class);
        var annotationCount = (sender == null ? 0 : 1) + (arg == null ? 0 : 1) + (flag == null ? 0 : 1);

        if (annotationCount > 1) {
            error("Command parameters must have at most one of @Sender, @Arg, or @Flag", parameter);
            return false;
        }

        var typeName = parameter.asType().toString();
        var stringSequence = SupportedCommandTypes.isStringSequenceType(typeName);
        var inferredKind =
                annotationCount == 0 ? inferParameterKind(parameter, subcommand, typeName, stringSequence) : null;
        if (annotationCount == 0 && inferredKind.isEmpty()) {
            return false;
        }
        var kind = annotationCount == 0
                ? inferredKind.get()
                : sender != null
                        ? ParameterModel.Kind.SENDER
                        : arg != null ? ParameterModel.Kind.ARGUMENT : ParameterModel.Kind.FLAG;

        if (greedy != null && !"java.lang.String".equals(typeName) && !stringSequence) {
            error("@Greedy is only supported for java.lang.String, String[], and List<String> arguments", parameter);
            return false;
        }

        if (kind == ParameterModel.Kind.FLAG && !"boolean".equals(typeName) && !"java.lang.Boolean".equals(typeName)) {
            error("MVP @Flag parameters must be boolean or java.lang.Boolean", parameter);
            return false;
        }

        var min = min(parameter);
        var max = max(parameter);
        if (arg != null && (min != null || max != null) && !SupportedCommandTypes.isNumericType(typeName)) {
            warning("@Min, @Max and @Range are only applied to numeric arguments", parameter);
            min = null;
            max = null;
        }

        if (min != null && max != null && min > max) {
            error("Argument minimum must be <= maximum", parameter);
            return false;
        }

        String suggestionMethodName = null;
        if (arg != null && kind == ParameterModel.Kind.ARGUMENT) {
            var suggestionName = suggestionName(arg, suggestions, methodSuggestions);
            if (!suggestionName.isBlank()) {
                suggestionMethodName = suggestionMethods.get(suggestionName);
                if (suggestionMethodName == null) {
                    error("No @SuggestionProvider found for suggestions: " + suggestionName, parameter);
                    return false;
                }
            }
        }

        subcommand.addParameter(new ParameterModel(
                parameterName(parameter, arg, flag, kind),
                typeName,
                kind,
                optional != null || stringSequence,
                optional == null ? null : optional.defaultValue(),
                greedy != null || stringSequence,
                min,
                max,
                flag == null ? '\0' : flag.shorthand(),
                suggestionMethodName));
        return true;
    }

    private Optional<ParameterModel.Kind> inferParameterKind(
            VariableElement parameter, SubcommandModel subcommand, String typeName, boolean stringSequence) {
        if (stringSequence) {
            return Optional.of(ParameterModel.Kind.ARGUMENT);
        }
        if (isUnannotatedSender(subcommand, typeName)) {
            return Optional.of(ParameterModel.Kind.SENDER);
        }
        if ("boolean".equals(typeName) || "java.lang.Boolean".equals(typeName)) {
            error("Unannotated boolean parameters are ambiguous; use @Flag or @Arg", parameter);
            return Optional.empty();
        }
        if (SupportedCommandTypes.isBuiltInArgumentType(typeName)) {
            return Optional.of(ParameterModel.Kind.ARGUMENT);
        }

        error("Unannotated custom parameters are ambiguous; use @Sender or @Arg", parameter);
        return Optional.empty();
    }

    private boolean isUnannotatedSender(SubcommandModel subcommand, String typeName) {
        return subcommand.getParameters().isEmpty() && !SupportedCommandTypes.isBuiltInArgumentType(typeName);
    }

    private String parameterName(VariableElement parameter, Arg arg, Flag flag, ParameterModel.Kind kind) {
        return switch (kind) {
            case SENDER -> parameter.getSimpleName().toString();
            case ARGUMENT -> arg == null ? parameter.getSimpleName().toString() : arg.value();
            case FLAG -> flag.value();
        };
    }

    private String suggestionName(Arg arg, Suggestions suggestions, Suggestions methodSuggestions) {
        if (suggestions != null && !suggestions.value().isBlank()) {
            return suggestions.value();
        }
        if (!arg.suggests().isBlank()) {
            return arg.suggests();
        }
        return methodSuggestions == null ? "" : methodSuggestions.value();
    }

    private boolean validateGreedyPosition(SubcommandModel subcommand, Element element) {
        var commandParameters = subcommand.getParameters().stream()
                .filter(parameter -> parameter.getKind() == ParameterModel.Kind.ARGUMENT
                        || parameter.getKind() == ParameterModel.Kind.FLAG)
                .toList();
        for (int index = 0; index < commandParameters.size(); index++) {
            var parameter = commandParameters.get(index);
            if (parameter.isGreedy() && index != commandParameters.size() - 1) {
                error("@Greedy arguments must be the last command argument", element);
                return false;
            }
        }
        return true;
    }

    private boolean validateGreedyFlags(SubcommandModel subcommand, Element element) {
        var hasGreedyArgument = subcommand.getParameters().stream().anyMatch(ParameterModel::isGreedy);
        var hasFlag = subcommand.getParameters().stream()
                .anyMatch(parameter -> parameter.getKind() == ParameterModel.Kind.FLAG);
        if (hasGreedyArgument && hasFlag) {
            error("@Greedy arguments cannot be combined with @Flag parameters", element);
            return false;
        }
        return true;
    }

    private Double min(VariableElement parameter) {
        var range = parameter.getAnnotation(Range.class);
        var min = parameter.getAnnotation(Min.class);
        if (min != null) {
            return min.value();
        }
        return range == null ? null : range.min();
    }

    private Double max(VariableElement parameter) {
        var range = parameter.getAnnotation(Range.class);
        var max = parameter.getAnnotation(Max.class);
        if (max != null) {
            return max.value();
        }
        return range == null ? null : range.max();
    }

    private void error(String message, Element element) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }

    private void warning(String message, Element element) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, message, element);
    }
}
