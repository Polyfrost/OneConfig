package cc.polyfrost.oneconfig.internal.assets;

import cc.polyfrost.oneconfig.renderer.AssetLoader;

/**
 * An enum of SVGs used in OneConfig.
 *
 * @see cc.polyfrost.oneconfig.renderer.RenderManager#drawSvg(long, String, float, float, float, float, int)
 * @see AssetLoader
 */
public enum SVGs {
    ONECONFIG("/assets/oneconfig/icons/OneConfig.svg"),
    ONECONFIG_OFF("/assets/oneconfig/icons/OneConfigOff.svg"),
    COPYRIGHT_FILL("/assets/oneconfig/icons/CopyrightFill.svg"),
    APERTURE_FILL("/assets/oneconfig/icons/ApertureFill.svg"),
    ARROWS_CLOCKWISE_BOLD("/assets/oneconfig/icons/ArrowsClockwiseBold.svg"),
    FADERS_HORIZONTAL_BOLD("/assets/oneconfig/icons/FadersHorizontalBold.svg"),
    GAUGE_FILL("/assets/oneconfig/icons/GaugeFill.svg"),
    GEAR_SIX_FILL("/assets/oneconfig/icons/GearSixFill.svg"),
    MAGNIFYING_GLASS_BOLD("/assets/oneconfig/icons/MagnifyingGlassBold.svg"),
    NOTE_PENCIL_BOLD("/assets/oneconfig/icons/NotePencilBold.svg"),
    PAINT_BRUSH_BROAD_FILL("/assets/oneconfig/icons/PaintBrushBroadFill.svg"),
    USER_SWITCH_FILL("/assets/oneconfig/icons/UserSwitchFill.svg"),
    X_CIRCLE_BOLD("/assets/oneconfig/icons/XCircleBold.svg"),
    CARET_LEFT("/assets/oneconfig/icons/CaretLeftBold.svg"),
    CARET_RIGHT("/assets/oneconfig/icons/CaretRightBold.svg"),

    // OLD ICONS
    BOX("/assets/oneconfig/old-icons/Box.svg"),
    CHECKBOX_TICK("/assets/oneconfig/old-icons/CheckboxTick.svg"),
    CHECK_CIRCLE("/assets/oneconfig/old-icons/CheckCircle.svg"),
    CHEVRON_DOWN("/assets/oneconfig/old-icons/ChevronDown.svg"),
    CHEVRON_UP("/assets/oneconfig/old-icons/ChevronUp.svg"),
    COPY("/assets/oneconfig/old-icons/Copy.svg"),
    DROPDOWN_LIST("/assets/oneconfig/old-icons/DropdownList.svg"),
    ERROR("/assets/oneconfig/old-icons/Error.svg"),
    EYE("/assets/oneconfig/old-icons/Eye.svg"),
    EYE_OFF("/assets/oneconfig/old-icons/EyeOff.svg"),
    HEART_FILL("/assets/oneconfig/old-icons/HeartFill.svg"),
    HEART_OUTLINE("/assets/oneconfig/old-icons/HeartOutline.svg"),
    HELP_CIRCLE("/assets/oneconfig/old-icons/HelpCircle.svg"),
    HISTORY("/assets/oneconfig/old-icons/History.svg"),
    INFO_CIRCLE("/assets/oneconfig/old-icons/InfoCircle.svg"),
    KEYSTROKE("/assets/oneconfig/old-icons/Keystroke.svg"),
    PASTE("/assets/oneconfig/old-icons/Paste.svg"),
    POP_OUT("/assets/oneconfig/old-icons/PopOut.svg"),
    WARNING("/assets/oneconfig/old-icons/Warning.svg");

    public final String filePath;

    SVGs(String filePath) {
        this.filePath = filePath;
    }
}
