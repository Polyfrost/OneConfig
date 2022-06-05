package cc.polyfrost.oneconfig.api.events.event;

import net.minecraft.util.Timer;

/**
 * Called when the {@link Timer} is updated.
 * Can be used as an alternative to getting instances of {@link Timer}
 * via Mixin or Access Wideners / Transformers
 */
public class TimerUpdateEvent {
    /**
     * Whether the deltaTicks / renderPartialTicks was updated
     */
    public final boolean updatedDeltaTicks;
    /**
     * The {@link Timer} instance
     */
    public final Timer timer;

    public TimerUpdateEvent(Timer timer, boolean updatedDeltaTicks) {
        this.timer = timer;
        this.updatedDeltaTicks = updatedDeltaTicks;
    }
}