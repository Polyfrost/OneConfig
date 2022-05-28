package cc.polyfrost.oneconfig.utils.commands.annotations;

import cc.polyfrost.oneconfig.utils.commands.arguments.Arguments;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the specific parameter should capture all remaining arguments and itself
 * Can only be used on the last parameter, and only works with {@link cc.polyfrost.oneconfig.utils.commands.arguments.StringParser} by default.
 *
 * @see cc.polyfrost.oneconfig.utils.commands.arguments.StringParser#parse(Arguments)
 * @see Command
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface Greedy {
}
