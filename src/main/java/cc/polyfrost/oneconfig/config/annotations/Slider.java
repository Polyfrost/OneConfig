package cc.polyfrost.oneconfig.config.annotations;

import cc.polyfrost.oneconfig.config.data.OptionType;
import cc.polyfrost.oneconfig.internal.config.annotations.Option;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Option(type = OptionType.SLIDER)
public @interface Slider {
    String name();

    float min();

    float max();

    int step() default 0;

    String category() default "General";

    String subcategory() default "";
}
