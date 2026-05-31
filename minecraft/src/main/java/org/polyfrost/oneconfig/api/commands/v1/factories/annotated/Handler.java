package org.polyfrost.oneconfig.api.commands.v1.factories.annotated;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an executor inside a (sub)command.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Handler {

    /**
     * Define overrides for the name this executor is given in the command tree.
     * <p>
     * By default, the name of the method is used as the sole name.
     */
    String[] value() default {};

    String description() default "";

}
