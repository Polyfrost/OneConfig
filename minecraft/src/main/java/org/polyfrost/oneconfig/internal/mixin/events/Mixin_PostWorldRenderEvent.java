package org.polyfrost.oneconfig.internal.mixin.events;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.deftu.omnicore.api.client.render.OmniRenderTicks;
import dev.deftu.omnicore.api.client.render.stack.OmniPoseStack;
import dev.deftu.omnicore.api.client.render.stack.OmniPoseStacks;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.PostWorldRenderEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class Mixin_PostWorldRenderEvent {

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void onRenderWorldPass(DeltaTracker deltaTracker, CallbackInfo ci) {
        OmniPoseStack stack = OmniPoseStacks.vanilla(new PoseStack());
        float partialTicks = OmniRenderTicks.get();
        EventManager.INSTANCE.post(new PostWorldRenderEvent(stack, partialTicks));
    }

}
