package com.hanielfialho.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface Flag {

    /**
     * Long flag name, for example {@code "force"} for {@code --force}.
     */
    String value();

    /**
     * Short flag alias, for example {@code 'f'} for {@code -f}.
     */
    char shorthand() default '\0';
}
