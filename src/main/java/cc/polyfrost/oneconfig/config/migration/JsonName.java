package cc.polyfrost.oneconfig.config.migration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <pre style="font-size: 12px">{@code public @interface JsonName}</pre>
 * This interface is used to specify a previous name for an element.<br>
 * For example, if you changed the name of a variable when you migrated/updated your mod to use OneConfig,
 * you can use this annotation to specify the previous name so that the Migrator can grab it. <br>
 * <b>This annotation uses dot notation to specify the full, case sensitive path to the old config field. See {@link JsonMigrator} for more information.</b>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JsonName {
    String value();
}
