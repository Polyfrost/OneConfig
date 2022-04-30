package io.polyfrost.oneconfig.config.data;

public enum OptionType {
    /**
     * Type: boolean
     */
    SWITCH,
    /**
     * Type: boolean
     */
    CHECKBOX,
    DUAL_OPTION,
    UNI_SELECTOR,
    /**
     * Type: String
     * Normal: 1x and 2x, Secure and Mutliline: 2x only
     */
    TEXT,
    SLIDER,
    COLOR,
    DROPDOWN,
    MULTI_DROPDOWN,
    INFO
}
