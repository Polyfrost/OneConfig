package org.polyfrost.oneconfig.internal.mixin.skia;

//? >= 26.1 {
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >= 26.2 {
/*import net.minecraft.client.Minecraft;

// 26.2 removed RenderSystem.flipFrame. The frame is now presented in Minecraft.renderFrame:
// GameRenderer.render -> GpuSurface.blitFromTexture(mainRenderTarget) -> GpuSurface.present().
// Flush queued Compose/Skia draws onto the back buffer right after the blit and before present.
@Mixin(Minecraft.class)
public class Mixin_SkiaFramePresent {

    @Inject(method = "renderFrame", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/GpuSurface;blitFromTexture(Lcom/mojang/blaze3d/systems/CommandEncoder;Lcom/mojang/blaze3d/textures/GpuTextureView;)V"))
    private void impl$onPresentVk(boolean tick, CallbackInfo ci) {
        if (SkiaCtx.INSTANCE.isVulkanMode()) {
            SkiaCtx.INSTANCE.draw();
        }
    }

    @Inject(method = "renderFrame", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/GpuSurface;blitFromTexture(Lcom/mojang/blaze3d/systems/CommandEncoder;Lcom/mojang/blaze3d/textures/GpuTextureView;)V",
            shift = At.Shift.AFTER))
    private void impl$onPresent(boolean tick, CallbackInfo ci) {
        if (!SkiaCtx.INSTANCE.isVulkanMode()) {
            SkiaCtx.INSTANCE.draw();
        }
    }
}
*///? } else {

import com.mojang.blaze3d.systems.RenderSystem;

/**
 * 26.1 removed {@code Window.updateDisplay}, which is where {@link Mixin_SkiaFrame} used to flush the
 * GL Skia surface. The frame is now presented via {@code RenderSystem.flipFrame} -> {@code GpuDevice.presentFrame},
 * called right after {@code RenderTarget.blitToScreen} in {@code Minecraft.runTick}. Flush here so queued
 * Compose/Skia draws land on the back buffer before the swap.
 */
@Mixin(RenderSystem.class)
public class Mixin_SkiaFramePresent {

    @Inject(method = "flipFrame", at = @At("HEAD"), remap = false)
    private static void impl$onPresent(CallbackInfo ci) {
        if (!SkiaCtx.INSTANCE.isVulkanMode()) {
            SkiaCtx.INSTANCE.draw();
        }
    }
}
//? }
//? }
