package cc.polyfrost.oneconfig.hud;

import cc.polyfrost.oneconfig.config.annotations.Color;
import cc.polyfrost.oneconfig.config.annotations.Dropdown;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.core.OneColor;

abstract class TextHud extends Hud {
    @Color(
            name = "Text Color"
    )
    public OneColor color = new OneColor(255, 255, 255);

    @Switch(
            name = "Show in Chat"
    )
    public boolean showInChat;

    @Switch(
            name = "Show in F3 (Debug)"
    )
    public boolean showInDebug;

    @Switch(
            name = "Show in GUIs"
    )
    public boolean showInGuis = true;

    @Dropdown(
            name = "Text Type",
            options = {"No Shadow", "Shadow", "Full Shadow"}
    )
    public int textType = 0;


    public TextHud(boolean enabled, int x, int y) {
        super(enabled, x, y);
    }
}
