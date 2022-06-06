package cc.polyfrost.oneconfig.config.annotations;

import cc.polyfrost.oneconfig.config.data.OptionType;
import cc.polyfrost.oneconfig.internal.config.annotations.Option;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Option(type = OptionType.TEXT)
public @interface Text {
    String name();

    String placeholder() default "";

    boolean secure() default false;

    boolean multiline() default false;

    int size() default 1;

    String category() default "General";

    String subcategory() default "";
}
