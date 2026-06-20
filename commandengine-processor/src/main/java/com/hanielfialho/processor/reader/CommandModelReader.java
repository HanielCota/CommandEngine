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
import com.hanielfialho.processor.model.SuggestionMethodModel;
import com.hanielfialho.processor.resolver.SuggestionMethodResolver;
import com.hanielfialho.processor.validation.SupportedCommandTypes;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;

public final class CommandModelReader {

    private static final int MAX_FLAGS_PER_SUBCOMMAND = 5;

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
        if (!isValidCommandName(annotation.name())) {
            error("@Command name contains invalid characters: " + annotation.name(), element);
            return Optional.empty();
        }
        if (!validateAliases(annotation.name(), annotation.aliases(), element)) {
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
        var subcommandPaths = new HashSet<String>();

        for (var enclosed : typeElement.getEnclosedElements()) {
            var subcommand = readSubcommand(enclosed, annotation.permission(), suggestionMethods);
            if (subcommand.isEmpty()) {
                continue;
            }
            if (!subcommandPaths.add(subcommand.get().getPath())) {
                error("Duplicate @Subcommand path: " + subcommand.get().getPath(), enclosed);
                return Optional.empty();
            }
            model.addSubcommand(subcommand.get());
        }
        if (model.getSubcommands().isEmpty()) {
            error("@Command classes must declare at least one @Subcommand or onCommand handler", element);
            return Optional.empty();
        }

        return Optional.of(new CommandDefinition(typeElement, model));
    }

    private Optional<SubcommandModel> readSubcommand(
            Element element, String commandPermission, Map<String, SuggestionMethodModel> suggestionMethods) {
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
        if (method.getModifiers().contains(Modifier.PRIVATE)) {
            error("@Subcommand handler must not be private", element);
            return Optional.empty();
        }
        if (method.getModifiers().contains(Modifier.STATIC)) {
            error("@Subcommand handler must not be static", element);
            return Optional.empty();
        }
        if (hasCheckedExceptions(method)) {
            error("@Subcommand handler must not declare checked exceptions", element);
            return Optional.empty();
        }
        var exec = element.getAnnotation(Execute.class);
        var methodSuggestions = element.getAnnotation(Suggestions.class);
        var subcommandPath = conventionRootHandler ? "" : sub.value();
        if (!isValidSubcommandPath(subcommandPath)) {
            error("@Subcommand path contains invalid characters or segments", element);
            return Optional.empty();
        }
        var returnType = method.getReturnType();
        var returnsVoid = returnType.getKind() == TypeKind.VOID;
        if (!returnsVoid && !isValidIntReturnType(returnType)) {
            error("@Subcommand handler must return void or int", element);
            return Optional.empty();
        }
        var subcommandPermission =
                conventionRootHandler || sub.permission().isBlank() ? commandPermission : sub.permission();
        if (!returnsVoid && exec != null && exec.async()) {
            error("@Execute(async = true) requires a void command handler", element);
            return Optional.empty();
        }
        var subcommand = new SubcommandModel(
                element.getSimpleName().toString(),
                subcommandPath,
                subcommandPermission,
                conventionRootHandler ? "" : sub.description(),
                returnsVoid && exec != null && exec.async(),
                returnsVoid);

        var parameterNames = new HashSet<String>();
        var shorthandFlags = new HashSet<Character>();
        for (var parameter : method.getParameters()) {
            if (!readParameter(
                    parameter, subcommand, suggestionMethods, methodSuggestions, parameterNames, shorthandFlags)) {
                return Optional.empty();
            }
        }
        if (!validateGreedyPosition(subcommand, element)) {
            return Optional.empty();
        }
        if (!validateGreedyFlags(subcommand, element)) {
            return Optional.empty();
        }
        if (!validateOptionalArgumentPosition(subcommand, element)) {
            return Optional.empty();
        }
        if (!validateFlagCount(subcommand, element)) {
            return Optional.empty();
        }
        return Optional.of(subcommand);
    }

    private boolean readParameter(
            VariableElement parameter,
            SubcommandModel subcommand,
            Map<String, SuggestionMethodModel> suggestionMethods,
            Suggestions methodSuggestions,
            Set<String> parameterNames,
            Set<Character> shorthandFlags) {
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
        if (arg != null && arg.value().isBlank()) {
            error("@Arg value must not be blank", parameter);
            return false;
        }
        if (flag != null && flag.value().isBlank()) {
            error("@Flag value must not be blank", parameter);
            return false;
        }
        if (flag != null && flag.shorthand() != '\0' && Character.isWhitespace(flag.shorthand())) {
            error("@Flag shorthand must not be whitespace", parameter);
            return false;
        }
        if (arg != null && !isValidCommandName(arg.value())) {
            error("@Arg value contains invalid characters: " + arg.value(), parameter);
            return false;
        }
        if (flag != null && !isValidCommandName(flag.value())) {
            error("@Flag value contains invalid characters: " + flag.value(), parameter);
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

        if (kind == ParameterModel.Kind.SENDER && parameter.asType().getKind().isPrimitive()) {
            error("@Sender parameters must not be primitive types", parameter);
            return false;
        }
        if (kind == ParameterModel.Kind.SENDER && (optional != null || greedy != null)) {
            error("@Optional and @Greedy are not allowed on @Sender parameters", parameter);
            return false;
        }
        if (kind == ParameterModel.Kind.FLAG && optional != null) {
            error("@Optional is not allowed on @Flag parameters", parameter);
            return false;
        }
        if (kind == ParameterModel.Kind.FLAG && greedy != null) {
            error("@Greedy is not allowed on @Flag parameters", parameter);
            return false;
        }
        if (kind == ParameterModel.Kind.FLAG && flag.shorthand() != '\0' && !shorthandFlags.add(flag.shorthand())) {
            error("Duplicate @Flag shorthand: " + flag.shorthand(), parameter);
            return false;
        }
        if (optional != null
                && kind == ParameterModel.Kind.ARGUMENT
                && !SupportedCommandTypes.isBuiltInArgumentType(typeName)) {
            if (optional.defaultValue().isBlank()) {
                error("@Optional defaultValue is required for custom argument types", parameter);
                return false;
            }
        }
        if (optional != null
                && kind == ParameterModel.Kind.ARGUMENT
                && ("boolean".equals(typeName) || "java.lang.Boolean".equals(typeName))
                && !optional.defaultValue().isEmpty()
                && !"true".equals(optional.defaultValue())
                && !"false".equals(optional.defaultValue())) {
            error("@Optional defaultValue for boolean must be 'true' or 'false'", parameter);
            return false;
        }

        var parameterName = parameterName(parameter, arg, flag, kind);
        if (kind != ParameterModel.Kind.SENDER && !parameterNames.add(parameterName)) {
            error("Duplicate parameter name: " + parameterName, parameter);
            return false;
        }

        if (greedy != null && !"java.lang.String".equals(typeName) && !stringSequence) {
            error("@Greedy is only supported for java.lang.String, String[], and List<String> arguments", parameter);
            return false;
        }

        if (kind == ParameterModel.Kind.FLAG && !"boolean".equals(typeName) && !"java.lang.Boolean".equals(typeName)) {
            error("MVP @Flag parameters must be boolean or java.lang.Boolean", parameter);
            return false;
        }

        var min = min(parameter, arg);
        var max = max(parameter, arg);
        if (arg != null && (min != null || max != null) && !SupportedCommandTypes.isNumericType(typeName)) {
            warning("@Min, @Max and @Range are only applied to numeric arguments", parameter);
            min = null;
            max = null;
        }

        if (min != null && max != null && min > max) {
            error("Argument minimum must be <= maximum", parameter);
            return false;
        }

        int minLength = arg == null ? 0 : arg.minLength();
        int maxLength = arg == null ? Integer.MAX_VALUE : arg.maxLength();
        if (minLength < 0 || maxLength < 0) {
            error("@Arg minLength and maxLength must be >= 0", parameter);
            return false;
        }
        if (minLength > maxLength) {
            error("@Arg minLength must be <= maxLength", parameter);
            return false;
        }
        if (arg != null && (minLength != 0 || maxLength != Integer.MAX_VALUE) && !"java.lang.String".equals(typeName)) {
            warning("@Arg minLength and maxLength are only applied to java.lang.String arguments", parameter);
            minLength = 0;
            maxLength = Integer.MAX_VALUE;
        }

        String suggestionMethodName = null;
        boolean asyncSuggestions = false;
        if (kind == ParameterModel.Kind.ARGUMENT) {
            var suggestionName = suggestionName(arg, suggestions, methodSuggestions);
            if (!suggestionName.isBlank()) {
                var suggestionMethod = suggestionMethods.get(suggestionName);
                if (suggestionMethod == null) {
                    error("No @SuggestionProvider found for suggestions: " + suggestionName, parameter);
                    return false;
                }
                suggestionMethodName = suggestionMethod.methodName();
                asyncSuggestions = suggestionMethod.async();
            }
        }

        if (optional != null && !validateDefaultValue(typeName, optional.defaultValue(), parameter)) {
            return false;
        }

        subcommand.addParameter(new ParameterModel(
                parameterName(parameter, arg, flag, kind),
                typeName,
                kind,
                optional != null,
                optional == null ? null : optional.defaultValue(),
                greedy != null || stringSequence,
                min,
                max,
                minLength,
                maxLength,
                flag == null ? '\0' : flag.shorthand(),
                suggestionMethodName,
                asyncSuggestions));
        return true;
    }

    private boolean validateAliases(String name, String[] aliases, Element element) {
        var seenAliases = new HashSet<String>();
        var normalizedName = normalizeCommandLabel(name);
        for (String alias : aliases) {
            if (alias.isBlank()) {
                error("@Command aliases must not contain blank values", element);
                return false;
            }
            if (!isValidCommandName(alias)) {
                error("@Command alias contains invalid characters: " + alias, element);
                return false;
            }
            var normalizedAlias = normalizeCommandLabel(alias);
            if (normalizedAlias.equals(normalizedName)) {
                error("@Command alias must not be equal to the command name: " + alias, element);
                return false;
            }
            if (!seenAliases.add(normalizedAlias)) {
                error("Duplicate @Command alias: " + alias, element);
                return false;
            }
        }
        return true;
    }

    private boolean isValidIntReturnType(javax.lang.model.type.TypeMirror returnType) {
        return returnType.toString().equals("int") || returnType.toString().equals("java.lang.Integer");
    }

    private boolean validateDefaultValue(String typeName, String defaultValue, Element element) {
        if (defaultValue == null || defaultValue.isBlank()) {
            return true;
        }
        try {
            switch (typeName) {
                case "int", "java.lang.Integer" -> Integer.parseInt(defaultValue);
                case "long", "java.lang.Long" -> Long.parseLong(defaultValue);
                case "float", "java.lang.Float" -> Float.parseFloat(defaultValue);
                case "double", "java.lang.Double" -> Double.parseDouble(defaultValue);
                case "boolean", "java.lang.Boolean" -> Boolean.parseBoolean(defaultValue);
                default -> {
                    // String and custom types accept any literal value.
                }
            }
            return true;
        } catch (NumberFormatException exception) {
            error("Invalid default value \"" + defaultValue + "\" for type " + typeName, element);
            return false;
        }
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
        if (arg != null && !arg.suggests().isBlank()) {
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

    private boolean validateFlagCount(SubcommandModel subcommand, Element element) {
        long flagCount = subcommand.getParameters().stream()
                .filter(parameter -> parameter.getKind() == ParameterModel.Kind.FLAG)
                .count();
        if (flagCount > MAX_FLAGS_PER_SUBCOMMAND) {
            error("Subcommands support at most " + MAX_FLAGS_PER_SUBCOMMAND + " @Flag parameters", element);
            return false;
        }
        return true;
    }

    private boolean validateOptionalArgumentPosition(SubcommandModel subcommand, Element element) {
        var arguments = subcommand.getParameters().stream()
                .filter(parameter -> parameter.getKind() == ParameterModel.Kind.ARGUMENT)
                .toList();
        for (int index = 0; index < arguments.size() - 1; index++) {
            if (arguments.get(index).isOptional()) {
                error("@Optional arguments must come after required arguments", element);
                return false;
            }
        }
        return true;
    }

    private Double min(VariableElement parameter, Arg arg) {
        var range = parameter.getAnnotation(Range.class);
        var min = parameter.getAnnotation(Min.class);
        if (min != null) {
            return min.value();
        }
        if (range != null) {
            return range.min();
        }
        return arg == null || Double.isInfinite(arg.min()) ? null : arg.min();
    }

    private Double max(VariableElement parameter, Arg arg) {
        var range = parameter.getAnnotation(Range.class);
        var max = parameter.getAnnotation(Max.class);
        if (max != null) {
            return max.value();
        }
        if (range != null) {
            return range.max();
        }
        return arg == null || Double.isInfinite(arg.max()) ? null : arg.max();
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

    private boolean isValidCommandName(String name) {
        return name != null && !name.isBlank() && name.chars().allMatch(this::isValidCommandChar);
    }

    private String normalizeCommandLabel(String name) {
        return name.toLowerCase(java.util.Locale.ROOT);
    }

    private boolean isValidCommandChar(int codePoint) {
        return Character.isLetterOrDigit(codePoint) || codePoint == '_' || codePoint == '-';
    }

    private boolean isValidSubcommandPath(String path) {
        if (path == null || path.isBlank()) {
            return true;
        }
        for (String part : path.trim().split("\\s+")) {
            if (!isValidCommandName(part)) {
                return false;
            }
        }
        return true;
    }

    private void error(String message, Element element) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }

    private void warning(String message, Element element) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, message, element);
    }
}
