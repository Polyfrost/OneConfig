package cc.polyfrost.oneconfig.config.annotations;

import cc.polyfrost.oneconfig.config.data.InfoType;
import cc.polyfrost.oneconfig.config.data.OptionType;

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
    String category() default "General";

    /**
     * The subcategory of the component (displayed as header)
     */
    String subcategory() default "";

    /**
     * The width of the option (1 = half width, 2 = full width)
     */
    int size() default 1;

    /**
     * A String array of all the possible values for the UniSelector, dropdownList, and ComboBox.
     * Also used in the DualOption slider, index 0 is the left, index 1 is the right; for example:
     * {"Option 1", "Option 2"}
     */
    String[] options() default {};

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
     * Minimum value of slider
     */
    float min() default 0;

    /**
     * The maximum value of the slider
     */
    float max() default 0;

    /**
     * Steps of slider (0 for no steps)
     */
    int step() default 0;

    /**
     * Option for info option type
     */
    InfoType infoType() default InfoType.INFO;

    /**
     * Text displayed inside button
     */
    String buttonText() default "Activate";
}
