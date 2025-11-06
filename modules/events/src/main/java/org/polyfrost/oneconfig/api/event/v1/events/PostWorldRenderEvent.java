package org.polyfrost.oneconfig.api.event.v1.events;

import dev.deftu.omnicore.api.client.render.stack.OmniPoseStack;

public class PostWorldRenderEvent implements Event {
    private final OmniPoseStack stack;
    private final float partialTicks;

    public PostWorldRenderEvent(OmniPoseStack stack, float partialTicks) {
        this.stack = stack;
        this.partialTicks = partialTicks;
    }

    public OmniPoseStack getStack() {
        return stack;
    }

    public float getPartialTicks() {
        return partialTicks;
    }
}
