package cc.polyfrost.oneconfig.config.migration.vigilance;

import java.lang.reflect.Field;

public interface Migrator {
    /**
     * @param field       The field of the option
     * @param name        The name of the option
     * @param category    The category of the option
     * @param subcategory The subcategory of the option
     * @return Value of the option, null if not found
     */
    Object getValue(Field field, String name, String category, String subcategory);
}
