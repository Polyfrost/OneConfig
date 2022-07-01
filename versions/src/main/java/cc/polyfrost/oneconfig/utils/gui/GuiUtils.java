package cc.polyfrost.oneconfig.utils.gui;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.RenderEvent;
import cc.polyfrost.oneconfig.events.event.Stage;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.libs.eventbus.Subscribe;
import cc.polyfrost.oneconfig.libs.universal.UMinecraft;
import cc.polyfrost.oneconfig.libs.universal.UScreen;
import cc.polyfrost.oneconfig.utils.TickDelay;
import net.minecraft.client.gui.GuiScreen;

import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * A class containing utility methods for working with GuiScreens.
 */
public final class GuiUtils {
    private static long time = -1L;
    private static long deltaTime = 17L;
    private static final Deque<Optional<GuiScreen>> screenQueue = new ConcurrentLinkedDeque<>();

    static {
        EventManager.INSTANCE.register(new GuiUtils());
    }

    /**
     * Displays a screen after a tick, preventing mouse sync issues.
     *
     * @param screen the screen to display.
     * @deprecated Not actually deprecated, but should not be used.
     */
    @Deprecated
    public static void displayScreen(Object screen) {
        displayScreen(((GuiScreen) screen));
    }

    /**
     * Displays a screen after a tick, preventing mouse sync issues.
     *
     * @param screen the screen to display.
     */
    public static void displayScreen(GuiScreen screen) {
        //noinspection ConstantConditions
        displayScreen(screen, screen instanceof OneConfigGui ? 2 : 1);
    }

    /**
     * Displays a screen after the specified amount of ticks.
     *
     * @param screen the screen to display.
     * @param ticks the amount of ticks to wait for before displaying the screen.
     */
    public static void displayScreen(GuiScreen screen, int ticks) {
        Optional<GuiScreen> optional = Optional.of(screen);
        screenQueue.add(optional);
        new TickDelay(() -> {
            UScreen.displayScreen(screen);
            screenQueue.remove(optional);
        }, ticks);
    }

    public static Deque<Optional<GuiScreen>> getScreenQueue() {
        return screenQueue;
    }

    /**
     * Close the current open GUI screen.
     */
    public static void closeScreen() {
        UScreen.displayScreen(null);
    }

    /**
     * Gets the delta time (in milliseconds) between frames.
     * <p><b>
     * Not to be confused with Minecraft deltaTicks / renderPartialTicks, which can be gotten via
     * {@link cc.polyfrost.oneconfig.events.event.TimerUpdateEvent}
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
