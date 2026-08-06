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

    //~ if >= 26.1 'runTick' -> 'renderFrame'
    @Inject(method = "renderFrame", at = @At(value = "INVOKE",
            //? if >= 26.2 {
            target = "Lcom/mojang/blaze3d/systems/GpuSurface;present()V"
            //?} elif 26.1 {
            /*target = "Lcom/mojang/blaze3d/systems/RenderSystem;flipFrame(Lcom/mojang/blaze3d/TracyFrameCapture;)V"
            *///?} elif >= 1.21.4 {
            /*target = "Lcom/mojang/blaze3d/platform/Window;updateDisplay(Lcom/mojang/blaze3d/TracyFrameCapture;)V"
            *///?} else
            //target = "Lcom/mojang/blaze3d/platform/Window;updateDisplay()V"
    ))
    private void preFramebufferRenderCallback(CallbackInfo ci) {
        EventManager.INSTANCE.post(FramebufferRenderEvent.Start.INSTANCE);
    }

    //~ if >= 26.1 'runTick' -> 'renderFrame'
    @Inject(method = "renderFrame", at = @At(value = "INVOKE",
            //? if >= 26.2 {
            target = "Lcom/mojang/blaze3d/systems/GpuSurface;present()V",
            //?} elif 26.1 {
            /*target = "Lcom/mojang/blaze3d/systems/RenderSystem;flipFrame(Lcom/mojang/blaze3d/TracyFrameCapture;)V",
            *///?} elif >= 1.21.4 {
            /*target = "Lcom/mojang/blaze3d/platform/Window;updateDisplay(Lcom/mojang/blaze3d/TracyFrameCapture;)V",
            *///?} else
            //target = "Lcom/mojang/blaze3d/platform/Window;updateDisplay()V",
            shift = At.Shift.AFTER
    ))
    private void postFramebufferRenderCallback(CallbackInfo ci) {
        EventManager.INSTANCE.post(FramebufferRenderEvent.End.INSTANCE);
    }

}
