package cc.polyfrost.oneconfig.internal.plugin.hooks;

import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.utils.gui.GuiUtils;

import java.util.Optional;

public class OptifineConfigHook {

    public static boolean shouldNotApplyFastRender() {
        if (Platform.getGuiPlatform().getCurrentScreen() instanceof OneConfigGui) {
            return true;
        }
        for (Optional screen : GuiUtils.getScreenQueue()) {
            if (screen.isPresent() && screen.get() instanceof OneConfigGui) {
                return true;
            }
        }
        return false;
    }
}
