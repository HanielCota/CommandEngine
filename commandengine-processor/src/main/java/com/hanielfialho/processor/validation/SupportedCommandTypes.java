package com.hanielfialho.processor.validation;

public final class SupportedCommandTypes {

    private SupportedCommandTypes() {}

    public static boolean isBuiltInArgumentType(String typeName) {
        return isNumericType(typeName)
                || "java.lang.String".equals(typeName)
                || isStringSequenceType(typeName)
                || "boolean".equals(typeName)
                || "java.lang.Boolean".equals(typeName);
    }

    public static boolean isStringSequenceType(String typeName) {
        return switch (typeName) {
            case "java.lang.String[]", "java.util.List<java.lang.String>" -> true;
            default -> false;
        };
    }

    public static boolean isNumericType(String typeName) {
        return switch (typeName) {
            case "int",
                    "java.lang.Integer",
                    "long",
                    "java.lang.Long",
                    "float",
                    "java.lang.Float",
                    "double",
                    "java.lang.Double" -> true;
            default -> false;
        };
    }
}
