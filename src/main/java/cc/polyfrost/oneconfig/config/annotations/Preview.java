package cc.polyfrost.oneconfig.config.annotations;

import cc.polyfrost.oneconfig.config.data.PageLocation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Preview {
    PageLocation location();
    String name();
    String category() default "General";
    String subcategory() default "";

}
