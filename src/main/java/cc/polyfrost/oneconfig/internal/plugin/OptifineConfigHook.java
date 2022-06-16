package cc.polyfrost.oneconfig.internal.plugin;

import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.libs.universal.UScreen;
import cc.polyfrost.oneconfig.utils.gui.GuiUtils;
import net.minecraft.client.gui.GuiScreen;

import java.util.Optional;

public class OptifineConfigHook {

    public static boolean shouldNotApplyFastRender() {
        if (UScreen.getCurrentScreen() instanceof OneConfigGui) {
            return true;
        }
        for (Optional<GuiScreen> screen : GuiUtils.getScreenQueue()) {
            if (screen.isPresent() && screen.get() instanceof OneConfigGui) {
                return true;
            }
        }
        return false;
    }
}
