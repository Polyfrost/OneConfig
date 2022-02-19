package io.polyfrost.oneconfig.themes;

public enum ThemeElement {
    DISCORD("textures/icons/discord.png", 128),
    DOCS("textures/icons/docs.png", 128),
    FEEDBACK("textures/icons/feedback.png", 128),
    GUIDE("textures/icons/guide.png", 128),
    HUD_SETTINGS("textures/icons/hudsettings.png", 128),
    MOD_SETTINGS("textures/icons/modsettings.png", 128),
    STORE("textures/icons/store.png", 128),
    THEMES("textures/icons/themes.png", 128),
    UPDATE("textures/icons/update.png", 128),

    BACK_ARROW("textures/smallicons/backarrow.png", 32),
    CLOSE("textures/smallicons/close.png", 32),
    FORWARD_ARROW("textures/smallicons/forward.png", 32),
    HOME("textures/smallicons/home.png", 32),
    MAGNIFY("textures/smallicons/magnify.png", 32),
    MINIMIZE("textures/smallicons/minimize.png", 32),
    SEARCH("textures/smallicons/backarrow.png", 32),

    ALL_MODS("textures/mod/allmods.png", 32),
    HUD_MODS("textures/mod/hudmods.png", 32),
    QOL_MODS("textures/mod/qolmods.png", 32),
    HYPIXEL("textures/mod/hypixel.png", 32),
    PERFORMANCE("textures/mod/performance.png", 32),
    PVP("textures/mod/pvp.png", 32),
    SKYBLOCK("textures/mod/skyblock.png", 32),
    UTILITIES("textures/mod/utilities.png", 32),

    LOGO("textures/logos/logo.png", 128),
    SMALL_LOGO("textures/logos/logo_small.png", 64),

    BUTTON_OFF("textures/window/button_off.png", 512),
    BUTTON_HOVER("textures/window/button_hover.png", 512),
    BUTTON_CLICK("textures/window/button_click.png", 512),

    BACKGROUND("textures/window/background.png", 1600);


    public final String location;
    public final int size;

    ThemeElement(String location, int size) {
        this.location = location;
        this.size = size;
    }
}
