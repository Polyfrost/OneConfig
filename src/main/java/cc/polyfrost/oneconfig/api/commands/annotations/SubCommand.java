package cc.polyfrost.oneconfig.api.commands.annotations;

import cc.polyfrost.oneconfig.api.commands.CommandManager;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a subcommand. Can be stacked together.
 *
 * @see Command
 * @see CommandManager
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface SubCommand {
    /**
     * The name of the command.
     *
     * @return The name of the command.
     */
    String value();

    /**
     * The aliases of the command.
     *
     * @return The aliases of the command.
     */
    String[] aliases() default {};

    /**
     * The description of the command.
     *
     * @return The description of the command.
     */
    String description() default "";
}
