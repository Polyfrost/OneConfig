package cc.polyfrost.oneconfig.api.events.event;

/**
 * Called when external HUDs can be rendered.
 */
public class HudRenderEvent {
    /**
     * How much time has elapsed since the last tick, in ticks. Used for animations.
     */
    public final float deltaTicks;

    public HudRenderEvent(float deltaTicks) {
        this.deltaTicks = deltaTicks;
    }
}
