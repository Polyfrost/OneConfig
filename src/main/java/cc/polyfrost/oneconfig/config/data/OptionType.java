package cc.polyfrost.oneconfig.config.data;

public enum OptionType {
    /**
     * Type: boolean
     */
    SWITCH,
    /**
     * Type: boolean
     */
    CHECKBOX,
    /**
     * Type: boolean
     */
    DUAL_OPTION,
    /**
     * Type: int
     */
    UNI_SELECTOR,
    /**
     * Type: String
     * Normal: 1x and 2x, Secure and Mutliline: 2x only
     */
    TEXT,
    /**
     * Type: int or float
     */
    SLIDER,
    COLOR,
    /**
     * Type: int
     */
    DROPDOWN,
    //MULTI_DROPDOWN,
    /**
     * Type: String
     */
    INFO
}
