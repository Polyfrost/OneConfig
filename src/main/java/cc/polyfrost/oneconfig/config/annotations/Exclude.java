package cc.polyfrost.oneconfig.config.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excludes fields from being serialized or deserialized by OneConfig's Config and HUD
 * system.
 *
 * This can be used interchangeably with the transient modifier built into Java.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Exclude {
    ExcludeType type() default ExcludeType.ALL;

    enum ExcludeType {
        ALL,
        CONFIG,
        HUD
    }
}
