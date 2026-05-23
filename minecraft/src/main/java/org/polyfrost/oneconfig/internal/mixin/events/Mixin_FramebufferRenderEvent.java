package org.polyfrost.oneconfig.internal.mixin.events;

import net.minecraft.client.Minecraft;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.FramebufferRenderEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class Mixin_FramebufferRenderEvent {

    @Inject(method = "runTick", at = @At(value = "INVOKE",
            //? >= 1.21.4 {
            target = "Lcom/mojang/blaze3d/platform/Window;updateDisplay(Lcom/mojang/blaze3d/TracyFrameCapture;)V"
            //? } else
            //target = "Lcom/mojang/blaze3d/platform/Window;updateDisplay()V"
    ))
    private void preFramebufferRenderCallback(CallbackInfo ci) {
        EventManager.INSTANCE.post(FramebufferRenderEvent.Start.INSTANCE);
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE",
            //? >= 1.21.4 {
            target = "Lcom/mojang/blaze3d/platform/Window;updateDisplay(Lcom/mojang/blaze3d/TracyFrameCapture;)V",
            //? } else
            //target = "Lcom/mojang/blaze3d/platform/Window;updateDisplay()V",
            shift = At.Shift.AFTER
    ))
    private void postFramebufferRenderCallback(CallbackInfo ci) {
        EventManager.INSTANCE.post(FramebufferRenderEvent.End.INSTANCE);
    }

}
