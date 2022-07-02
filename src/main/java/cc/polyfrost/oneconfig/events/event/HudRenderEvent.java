package cc.polyfrost.oneconfig.events.event;

import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;

/**
 * Called when external HUDs can be rendered.
 */
public class HudRenderEvent {
    /**
     * How much time has elapsed since the last tick, in ticks. Used for animations.
     */
    public final float deltaTicks;
    public final UMatrixStack matrices;

    public HudRenderEvent(UMatrixStack matrices, float deltaTicks) {
        this.matrices = matrices;
        this.deltaTicks = deltaTicks;
    }
}
