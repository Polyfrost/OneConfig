package cc.polyfrost.oneconfig.events.event;

public class HudRenderEvent {
    public final float deltaTicks;

    public HudRenderEvent(float deltaTicks) {
        this.deltaTicks = deltaTicks;
    }
}
