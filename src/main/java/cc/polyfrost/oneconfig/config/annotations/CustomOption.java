package cc.polyfrost.oneconfig.config.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * For documentation please see: <a href="https://docs.polyfrost.cc/oneconfig/config/adding-options/custom-options">https://docs.polyfrost.cc/oneconfig/config/adding-options/custom-options</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
public @interface CustomOption {
    String id() default "";
}
