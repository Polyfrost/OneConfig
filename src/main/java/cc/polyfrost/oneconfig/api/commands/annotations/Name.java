package cc.polyfrost.oneconfig.api.commands.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the name of a parameter.
 *
 * @see Main
 * @see Command
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface Name {
    /**
     * The name of the parameter.
     *
     * @return The name of the parameter.
     */
    String value();
}
