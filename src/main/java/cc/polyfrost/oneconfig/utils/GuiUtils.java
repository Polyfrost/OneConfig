package cc.polyfrost.oneconfig.utils;

import cc.polyfrost.oneconfig.libs.universal.UScreen;
import net.minecraft.client.gui.GuiScreen;

/**
 * A class containing utility methods for working with GuiScreens.
 */
public class GuiUtils {

    /**
     * Displays a screen after a tick, preventing mouse sync issues.
     * @param screen the screen to display.
     */
    public static void displayScreen(GuiScreen screen) {
        new TickDelay(() -> UScreen.displayScreen(screen), 1);
    }
}
