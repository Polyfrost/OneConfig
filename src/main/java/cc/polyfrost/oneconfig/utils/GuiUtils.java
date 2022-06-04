package cc.polyfrost.oneconfig.utils;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.RenderEvent;
import cc.polyfrost.oneconfig.events.event.Stage;
import cc.polyfrost.oneconfig.libs.eventbus.Subscribe;
import cc.polyfrost.oneconfig.libs.universal.UMinecraft;
import cc.polyfrost.oneconfig.libs.universal.UScreen;
import net.minecraft.client.gui.GuiScreen;

/**
 * A class containing utility methods for working with GuiScreens.
 */
public final class GuiUtils {
    static {
        EventManager.INSTANCE.register(new GuiUtils());
    }
    private static long time = -1L;
    private static long deltaTime = 17L;

    /**
     * Displays a screen after a tick, preventing mouse sync issues.
     *
     * @param screen the screen to display.
     */
    public static void displayScreen(GuiScreen screen) {
        new TickDelay(() -> UScreen.displayScreen(screen), 1);
    }

    /** Close the current open GUI screen. */
    public static void closeScreen() {
        UScreen.displayScreen(null);
    }

    /**
     * Gets the delta time (in milliseconds) between frames.
     * <p><b>
     *     Not to be confused with Minecraft deltaTicks / renderPartialTicks, which can be gotten via
     *     {@link cc.polyfrost.oneconfig.events.event.TimerUpdateEvent}
     * </b></p>
     *
     * @return the delta time.
     */
    public static float getDeltaTime() {
        return deltaTime;
    }

    @Subscribe
    private void onRenderEvent(RenderEvent event) {
        if (event.stage == Stage.START) {
            if (time == -1) time = UMinecraft.getTime();
            else {
                long currentTime = UMinecraft.getTime();
                deltaTime = currentTime - time;
                time = currentTime;
            }
        }
    }
}
