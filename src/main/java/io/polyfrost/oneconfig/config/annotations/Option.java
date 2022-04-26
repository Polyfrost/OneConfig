package io.polyfrost.oneconfig.config.annotations;

import io.polyfrost.oneconfig.config.data.OptionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Option {
    /**
     * The name of the page that will be displayed to the user
     */
    String name();

    /**
     * The description of the page that will be displayed to the user
     */
    String description() default "";

    /**
     * The type of the option
     */
    OptionType type();

    /**
     * The category of the component
     */
    String category() default "general";

    /**
     * The subcategory of the component (displayed as header)
     */
    String subcategory();

    /**
     * The width of the option (1 = half width, 2 = full width)
     */
    int size() default 1;
}
