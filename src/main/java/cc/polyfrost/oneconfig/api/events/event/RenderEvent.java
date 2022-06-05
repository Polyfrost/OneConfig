package cc.polyfrost.oneconfig.api.events.event;

/**
 * Called when a game tick is started / ended, represented by a {@link Stage}
 */
public class RenderEvent {
    /**
     * Whether the tick is starting or ending.
     */
    public final Stage stage;

    /**
     * How much time has elapsed since the last tick, in ticks. Used for animations.
     */
    public final float deltaTicks;

    public RenderEvent(Stage stage, float deltaTicks) {
        this.stage = stage;
        this.deltaTicks = deltaTicks;
    }
}
