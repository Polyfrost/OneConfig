package cc.polyfrost.oneconfig.config.annotations;

import cc.polyfrost.oneconfig.config.data.PageLocation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigPage {
    /**
     * The name of the page that will be displayed to the user
     */
    String name();

    /**
     * If the page button is at the top or bottom of the page
     */
    PageLocation location();

    /**
     * The description of the page that will be displayed to the user
     */
    String description() default "";

    /**
     * The category of the page
     */
    String category() default "General";

    /**
     * The subcategory of the page
     */
    String subcategory() default "";
}
