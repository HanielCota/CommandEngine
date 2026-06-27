package com.hanielfialho.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface Arg {

    /**
     * Argument name.
     */
    String value();

    /**
     * Argument description used by help and documentation tooling.
     */
    String description() default "";

    /**
     * Method or class used to provide tab-complete suggestions.
     */
    String suggests() default "";

    /**
     * Minimum string length (only applied to {@code java.lang.String} arguments).
     */
    int minLength() default 0;

    int maxLength() default Integer.MAX_VALUE;

    double min() default Double.NEGATIVE_INFINITY;

    double max() default Double.POSITIVE_INFINITY;
}
