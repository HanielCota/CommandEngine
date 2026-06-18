package com.hanielfialho.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface Subcommand {

    /**
     * The subcommand path, for example {@code "reload"} or {@code "player kick"}.
     */
    String value();

    /**
     * Specific permission for this subcommand.
     * When blank, the permission is inherited from the owning {@link Command}.
     */
    String permission() default "";

    /**
     * Description for this branch.
     */
    String description() default "";
}
