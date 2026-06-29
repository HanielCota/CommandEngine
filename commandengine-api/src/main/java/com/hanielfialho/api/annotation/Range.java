package com.hanielfialho.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.jetbrains.annotations.NotNull;

/**
 * Constrains a numeric argument to a minimum and maximum value.
 * The annotation uses {@code double} for both bounds, but the actual
 * precision is determined by the argument's Java type ({@code int},
 * {@code long}, {@code float}, or {@code double}). Values outside
 * the target type's range are clamped by the generated Brigadier
 * argument at parse time.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface Range {

    double min();

    double max();

    /**
     * Validates that {@code min} is less than or equal to {@code max}.
     */
    final class Validator {
        private Validator() {}

        public static void validate(@NotNull Range range) {
            if (range.min() > range.max()) {
                throw new IllegalArgumentException(
                        "Range min (" + range.min() + ") must be <= max (" + range.max() + ")");
            }
        }
    }
}
