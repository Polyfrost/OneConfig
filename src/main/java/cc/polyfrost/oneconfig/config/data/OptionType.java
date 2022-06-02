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
    TEXT,
    /**
     * Type: int or float
     */
    SLIDER,
    /**
     * Type: OneColor
     */
    COLOR,
    /**
     * Type: int
     */
    DROPDOWN,
    /**
     * Type: doesn't matter
     */
    INFO,
    /**
     * Type: doesn't matter
     */
    HEADER,
    /**
     * Type: runnable
     */
    BUTTON,
    /**
     * Type: OneKeyBind
     */
    KEYBIND,
    /**
     * Type: ? extends BasicHud
     */
    HUD,
}
