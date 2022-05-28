package cc.polyfrost.oneconfig.events.event;

/**
 * Called when a game tick is started / ended, represented by a {@link Stage}
 */
public class TickEvent {
    /**
     * Whether the tick is starting or ending.
     */
    public final Stage stage;

    public TickEvent(Stage stage) {
        this.stage = stage;
    }
}
