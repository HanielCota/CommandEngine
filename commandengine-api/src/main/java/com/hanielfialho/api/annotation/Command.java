package com.hanielfialho.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a root command for the framework.
 * Processed at compile time to generate native adapters.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Command {

    /**
     * The primary command name, for example {@code "teleport"}.
     * This becomes the Brigadier root node.
     */
    String name();

    /**
     * Alternative command aliases, for example {@code "tp"}.
     */
    String[] aliases() default {};

    /**
     * Short description used by native help integrations.
     */
    String description() default "";

    /**
     * Base permission. Sources without this permission should not see or execute
     * the command.
     */
    String permission() default "";
}
