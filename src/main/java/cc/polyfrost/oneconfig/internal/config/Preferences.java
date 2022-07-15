package cc.polyfrost.oneconfig.internal.config;

import cc.polyfrost.oneconfig.config.annotations.KeyBind;
import cc.polyfrost.oneconfig.config.annotations.Slider;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.core.OneKeyBind;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.internal.gui.BlurHandler;
import cc.polyfrost.oneconfig.libs.universal.UKeyboard;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.utils.TickDelay;

public class Preferences extends InternalConfig {
    @Switch(
            name = "Enable Blur"
    )
    public static boolean enableBlur = true;

    @KeyBind(
            name = "OneConfig Keybind",
            size = 2
    )
    public static OneKeyBind oneConfigKeyBind = new OneKeyBind(() -> new TickDelay(() -> Platform.getGuiPlatform().setCurrentScreen(OneConfigGui.create()), 1), UKeyboard.KEY_RSHIFT);


    @Switch(
            name = "Use custom GUI scale",
            subcategory = "GUI Scale",
            size = 2
    )
    public static boolean enableCustomScale = false;

    @Slider(
            name = "Custom GUI scale",
            subcategory = "GUI Scale",
            min = 0.5f,
            max = 5f
    )
    public static float customScale = 1f;

    private static Preferences INSTANCE;

    public Preferences() {
        super("Preferences", "Preferences.json");
        initialize();
        addListener("enableBlur", () -> BlurHandler.INSTANCE.reloadBlur(Platform.getGuiPlatform().getCurrentScreen()));
        INSTANCE = this;
    }

    public static Preferences getInstance() {
        return INSTANCE == null ? (INSTANCE = new Preferences()) : INSTANCE;
    }
}
