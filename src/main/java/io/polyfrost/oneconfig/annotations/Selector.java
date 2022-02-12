package io.polyfrost.oneconfig.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Selector {
    String name();
    String description() default "";
    String[] options();
    int defaultSelection() default 0;
}
