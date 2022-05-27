package cc.polyfrost.oneconfig.events.event;

public class RenderEvent {
    public final Stage stage;
    public final float deltaTicks;

    public RenderEvent(Stage stage, float deltaTicks) {
        this.stage = stage;
        this.deltaTicks = deltaTicks;
    }
}
