package org.polyfrost.oneconfig.api.commands.v1.factories.annotated;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an executor inside a (sub)command
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Handler {

    /**
     * Overrides the name this executor is given in the command tree
     * <p>
     * Defaults to the method name
     */
    String[] value() default {};

    String description() default "";

}
