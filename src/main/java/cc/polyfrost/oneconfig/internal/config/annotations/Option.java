package cc.polyfrost.oneconfig.internal.config.annotations;

import cc.polyfrost.oneconfig.config.data.OptionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Option {
    OptionType type();
}
