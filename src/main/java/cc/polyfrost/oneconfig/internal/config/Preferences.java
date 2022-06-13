package cc.polyfrost.oneconfig.internal.config;

import cc.polyfrost.oneconfig.config.annotations.KeyBind;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.core.OneKeyBind;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.libs.universal.UKeyboard;
import cc.polyfrost.oneconfig.utils.gui.GuiUtils;

public class Preferences extends InternalConfig {
    @Switch(
            name = "Enable Blur"
    )
    public static boolean enableBlur = true;

    @KeyBind(
            name = "OneConfig Keybind",
            size = 2
    )
    public static OneKeyBind oneConfigKeyBind = new OneKeyBind(UKeyboard.KEY_RSHIFT);

    public Preferences() {
        super("Preferences", "Preferences.json");
        registerKeyBind(oneConfigKeyBind, () -> GuiUtils.displayScreen(OneConfigGui.create()));
    }
}
