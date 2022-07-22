package cc.polyfrost.oneconfig.config.migration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

public interface Migrator {
    /**
     * Get the value from its name, category, and subcategory. The target field is also supplied, which can be used to check for {@link MigrationName}.
     * The returned Object is intended to be a "Duck" object, and should be cast to the correct type. The Migrator should never return ClassCastExceptions.
     *
     * @param field       The target field of the option
     * @param name        The name of the option (has to be present)
     * @param category    The category of the option
     * @param subcategory The subcategory of the option
     * @return Value of the option, null if not found
     * @apiNote <b>The nullability of the subcategory or category depends on the implementation. Please check for @NotNull or @Nullable in your implementation.</b>
     */
    @Nullable
    Object getValue(Field field, @NotNull String name, String category, String subcategory);

    /**
     * Generate an option list used for the config to be migrated.
     * This is intended to be used in such a way that: <br>
     * <code>if(values == null) generateValues();</code>
     */
    void generateValues();

    /**
     * Parse the given String (a name, category, or subcategory) into the correct format for the target file.
     * For example, in the {@link VigilanceMigrator}, it replaces all spaces with underscores, like .toml files.
     *
     * @param value the input value
     * @return the input parsed correctly
     */
    @NotNull
    String parse(@NotNull String value);


}
