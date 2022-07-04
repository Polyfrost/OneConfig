package cc.polyfrost.oneconfig.internal.config.compatibility.vigilance;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface VigilanceName {
    String name();

    String category();

    String subcategory();
}
