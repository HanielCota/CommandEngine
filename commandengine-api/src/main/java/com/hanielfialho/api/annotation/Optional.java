package com.hanielfialho.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface Optional {

    /**
     * Default value used when the argument is not provided.
     * Non-String values are converted by the argument resolver.
     */
    String defaultValue() default "";
}
