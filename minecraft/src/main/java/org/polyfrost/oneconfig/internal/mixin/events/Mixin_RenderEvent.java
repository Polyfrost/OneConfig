package org.polyfrost.oneconfig.internal.mixin.events;

import dev.deftu.omnicore.api.client.render.OmniRenderTicks;
import dev.deftu.omnicore.api.client.render.OmniRenderingContext;
import net.minecraft.client.Minecraft;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.RenderEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class Mixin_RenderEvent {

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render(Lnet/minecraft/client/DeltaTracker;Z)V"))
    private void renderTickStartCallback(CallbackInfo ci) {
        RenderEvent e = RenderEvent.Pre.INSTANCE;
        e.deltaTicks = OmniRenderTicks.get();
        e.ctx = OmniRenderingContext.create();
        EventManager.INSTANCE.post(e);
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE",
            //#if MC >= 1.21.4
            //$$ target = "Lcom/mojang/blaze3d/platform/Window;updateDisplay(Lcom/mojang/blaze3d/TracyFrameCapture;)V"
            //#else
            target = "Lcom/mojang/blaze3d/platform/Window;updateDisplay()V"
            //#endif
    ))
    private void renderTickEndCallback(CallbackInfo ci) {
        RenderEvent e = RenderEvent.Post.INSTANCE;
        e.deltaTicks = OmniRenderTicks.get();
        e.ctx = OmniRenderingContext.create();
        EventManager.INSTANCE.post(e);
    }

}
