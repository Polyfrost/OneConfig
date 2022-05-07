package cc.polyfrost.oneconfig.lwjgl.image;

public enum Images {
    CHEVRON_ARROW("/assets/oneconfig/textures/gui/general/arrows/chevron.png"),
    DROPDOWN_ARROW("/assets/oneconfig/textures/gui/general/arrows/dropdown_arrow.png"),
    UP_ARROW("/assets/oneconfig/textures/gui/general/arrows/up_arrow.png"),
    CIRCLE_ARROW("/assets/oneconfig/textures/gui/general/arrows/circle_arrow.png"),

    CHECKMARK("/assets/oneconfig/textures/gui/general/configs/checkmark.png"),
    FAVORITE("/assets/oneconfig/textures/gui/general/configs/favorite_active.png"),
    FAVORITE_OFF("/assets/oneconfig/textures/gui/general/configs/favorite_inactive.png"),
    HIDE_EYE("/assets/oneconfig/textures/gui/general/configs/hide_eye.png"),
    HIDE_EYE_OFF("/assets/oneconfig/textures/gui/general/configs/hide_eye_off.png"),

    // TODO color picker ones
    COLOR_BASE("/assets/oneconfig/textures/gui/general/color/color_base.png"),
    COLOR_BASE_LONG("/assets/oneconfig/textures/gui/general/color/color_base_long.png"),
    COLOR_BASE_LARGE("/assets/oneconfig/textures/gui/general/color/color_base_large.png"),
    COLOR_WHEEL("/assets/oneconfig/textures/gui/general/color/color_wheel.png"),

    INFO("/assets/oneconfig/textures/gui/icons/alert/info.png"),
    SUCCESS("/assets/oneconfig/textures/gui/icons/alert/success.png"),
    WARNING("/assets/oneconfig/textures/gui/icons/alert/warning.png"),
    ERROR("/assets/oneconfig/textures/gui/icons/alert/error.png"),

    SHARE("/assets/oneconfig/textures/gui/general/nav/share.png"),
    LAUNCH("/assets/oneconfig/textures/gui/general/nav/launch.png"),
    SEARCH("/assets/oneconfig/textures/gui/general/nav/search.png"),
    MINIMIZE("/assets/oneconfig/textures/gui/general/nav/minimize.png"),
    CLOSE("/assets/oneconfig/textures/gui/general/nav/close.png"),
    HELP("/assets/oneconfig/textures/gui/general/nav/help.png"),
    COPY("/assets/oneconfig/textures/gui/general/nav/copy.png"),
    PASTE("/assets/oneconfig/textures/gui/general/nav/paste.png"),

    LOGO("/assets/oneconfig/textures/gui/general/logo.png"),

    HUD("/assets/oneconfig/textures/gui/icons/hud/hud.png"),
    HUD_SETTINGS("/assets/oneconfig/textures/gui/icons/hud/settings.png"),

    MOD_BOX("/assets/oneconfig/textures/gui/icons/mod/mod_box.png"),
    MODS("/assets/oneconfig/textures/gui/icons/mod/mods.png"),
    PERFORMANCE("/assets/oneconfig/textures/gui/icons/mod/performance.png"),

    DASHBOARD("/assets/oneconfig/textures/gui/icons/dashboard.png"),
    PREFERENCES("/assets/oneconfig/textures/gui/icons/preferences.png"),
    PROFILES("/assets/oneconfig/textures/gui/icons/profiles.png"),
    SCREENSHOT("/assets/oneconfig/textures/gui/icons/screenshot.png"),
    THEMES("/assets/oneconfig/textures/gui/icons/themes.png"),
    UPDATES("/assets/oneconfig/textures/gui/icons/updates.png"),
    ;

    public final String filePath;

    Images(String filePath) {
        this.filePath = filePath;
    }
}
