package cc.polyfrost.oneconfig.api.commands.annotations;

import cc.polyfrost.oneconfig.api.commands.CommandManager;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as the main method of a command.
 *
 * @see Command
 * @see SubCommand
 * @see CommandManager
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Main {
    String description() default "";

    int priority() default 1000;
}
