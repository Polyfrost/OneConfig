package io.polyfrost.oneconfig.config.annotations;

import io.polyfrost.oneconfig.config.data.InfoType;
import io.polyfrost.oneconfig.config.data.OptionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Option {
    /**
     * The name of the option that will be displayed to the user
     */
    String name();

    /**
     * The type of the option
     */
    OptionType type();

    /**
     * The category of the component
     */
    String category() default "general";

    /**
     * The subcategory of the component (displayed as header)
     */
    String subcategory();

    /** The name shown on the Left option of a DualOption slider. */
    String optionLeft() default "Option 1";

    /** The name shown on the Right option of a DualOption slider. */
    String optionRight() default "Option 2";

    /** A String array of all the possible values for the UniSelector, dropdownList, and ComboBox */
    String[] options() default {};

    /**
     * The width of the option (1 = half width, 2 = full width)
     */
    int size() default 1;

    /**
     * The placeholder in the text field
     */
    String placeholder() default "";

    /**
     * If the text field is secure or not
     */
    boolean secure() default false;

    /**
     * If the text field is multi line or not
     */
    boolean multiLine() default false;

    /**
     * Steps of slider (0 for no steps)
     */
    int step() default 0;

    /**
     * Option for info option type
     */
    InfoType infoType() default InfoType.INFO;
}
