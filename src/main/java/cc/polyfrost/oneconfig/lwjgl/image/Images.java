package cc.polyfrost.oneconfig.lwjgl.image;

public enum Images {
    CHEVRON_ARROW("/assets/oneconfig/icons/chevron.png"),
    DROPDOWN_ARROW("/assets/oneconfig/icons/dropdown_arrow.png"),
    UP_ARROW("/assets/oneconfig/icons/up_arrow.png"),
    CIRCLE_ARROW("/assets/oneconfig/icons/circle_arrow.png"),

    CHECKMARK("/assets/oneconfig/icons/checkmark.png"),
    FAVORITE("/assets/oneconfig/icons/favorite_active.png"),
    FAVORITE_OFF("/assets/oneconfig/icons/favorite_inactive.png"),
    HIDE_EYE("/assets/oneconfig/icons/hide_eye.png"),
    HIDE_EYE_OFF("/assets/oneconfig/icons/hide_eye_off.png"),
    KEYSTROKE("/assets/oneconfig/icons/keystroke.png"),

    // TODO color picker ones
    COLOR_BASE("/assets/oneconfig/colorui/color_base.png"),
    COLOR_BASE_LONG("/assets/oneconfig/colorui/color_base_long.png"),
    COLOR_BASE_LARGE("/assets/oneconfig/colorui/color_base_large.png"),
    COLOR_WHEEL("/assets/oneconfig/colorui/color_wheel.png"),
    HUE_GRADIENT("/assets/oneconfig/colorui/hue_gradient.png"),
    CLOSE_COLOR("/assets/oneconfig/colorui/close_color.png"),

    INFO("/assets/oneconfig/icons/info.png"),
    SUCCESS("/assets/oneconfig/icons/success.png"),
    WARNING("/assets/oneconfig/icons/warning.png"),
    ERROR("/assets/oneconfig/icons/error.png"),

    SHARE("/assets/oneconfig/icons/share.png"),
    LAUNCH("/assets/oneconfig/icons/launch.png"),
    SEARCH("/assets/oneconfig/icons/search.png"),
    MINIMIZE("/assets/oneconfig/icons/minimize.png"),
    CLOSE("/assets/oneconfig/icons/close.png"),
    HELP("/assets/oneconfig/icons/help.png"),
    COPY("/assets/oneconfig/icons/copy.png"),
    PASTE("/assets/oneconfig/icons/paste.png"),

    LOGO("/assets/oneconfig/icons/logo.png"),

    HUD("/assets/oneconfig/icons/hud.png"),
    HUD_SETTINGS("/assets/oneconfig/icons/settings.png"),

    MOD_BOX("/assets/oneconfig/icons/mod_box.png"),
    MODS("/assets/oneconfig/icons/mods.png"),
    PERFORMANCE("/assets/oneconfig/icons/performance.png"),

    DASHBOARD("/assets/oneconfig/icons/dashboard.png"),
    PREFERENCES("/assets/oneconfig/icons/preferences.png"),
    PROFILES("/assets/oneconfig/icons/profiles.png"),
    SCREENSHOT("/assets/oneconfig/icons/screenshot.png"),
    THEMES("/assets/oneconfig/icons/themes.png"),
    UPDATES("/assets/oneconfig/icons/updates.png"),
    ;

    public final String filePath;

    Images(String filePath) {
        this.filePath = filePath;
    }
}
