package cc.polyfrost.oneconfig.events.event;

import org.jetbrains.annotations.Nullable;

/**
 * Called when a screen is opened or closed.
 * If the screen is closed, {@link ScreenOpenEvent#screen} will be null.
 */
public class ScreenOpenEvent extends CancellableEvent {
    @Nullable
    public final Object screen;

    public ScreenOpenEvent(@Nullable Object screen) {
        this.screen = screen;
    }
}
