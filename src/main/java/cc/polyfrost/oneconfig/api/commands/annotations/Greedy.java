package cc.polyfrost.oneconfig.api.commands.annotations;

import cc.polyfrost.oneconfig.api.commands.arguments.Arguments;
import cc.polyfrost.oneconfig.api.commands.arguments.StringParser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the specific parameter should capture all remaining arguments and itself
 * Can only be used on the last parameter, and only works with {@link StringParser} by default.
 *
 * @see StringParser#parse(Arguments)
 * @see Command
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface Greedy {
}
