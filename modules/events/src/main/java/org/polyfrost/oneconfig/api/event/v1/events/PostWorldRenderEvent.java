package org.polyfrost.oneconfig.api.event.v1.events;

import dev.deftu.omnicore.api.client.render.stack.OmniMatrixStack;

public class PostWorldRenderEvent implements Event {
    private final OmniMatrixStack stack;
    private final float partialTicks;

    public PostWorldRenderEvent(OmniMatrixStack stack, float partialTicks) {
        this.stack = stack;
        this.partialTicks = partialTicks;
    }

    public OmniMatrixStack getStack() {
        return stack;
    }

    public float getPartialTicks() {
        return partialTicks;
    }
}
