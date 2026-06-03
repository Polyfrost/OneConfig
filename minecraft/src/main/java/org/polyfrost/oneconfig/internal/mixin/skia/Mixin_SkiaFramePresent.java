package org.polyfrost.oneconfig.internal.mixin.skia;

//? >= 26.1 {
import com.mojang.blaze3d.systems.RenderSystem;
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
