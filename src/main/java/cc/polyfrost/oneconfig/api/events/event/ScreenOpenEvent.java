package cc.polyfrost.oneconfig.api.events.event;

import net.minecraft.client.gui.GuiScreen;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a screen is opened or closed.
 * If the screen is closed, {@link ScreenOpenEvent#screen} will be null.
 */
public class ScreenOpenEvent extends CancellableEvent {
    @Nullable
    public final GuiScreen screen;

    public ScreenOpenEvent(@Nullable GuiScreen screen) {
        this.screen = screen;
    }
}
