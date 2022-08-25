package cc.polyfrost.oneconfig.utils.commands.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark a method as a subcommand. <br>
 * A subcommand can be called using any of the aliases in the annotation, AND its name (case-insensitive). <br><br>
 * It doesn't have to have any arguments, but they are recommended as they are used for better help messages for your users and for aliases. <br>
 * </pre>
 * <b>Usage on methods:</b>
 * <pre>{@code
 *  @SubCommand(description = "A command for doing something cool.", aliases = {"myc", "yes"})
 *  public void myCommand(int bob, float someParam) {
 *      // with this annotation, the description will be displayed on the help message.
 *      // also, because of the aliases, it can be called using /test mycommand, /test myc, or /test yes.
 *  }
 *
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubCommand {
    String description() default "";
    String[] aliases() default {};
}
