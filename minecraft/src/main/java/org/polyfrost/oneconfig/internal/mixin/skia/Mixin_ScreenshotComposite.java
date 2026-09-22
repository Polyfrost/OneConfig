package org.polyfrost.oneconfig.internal.mixin.skia;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Screenshot;
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

/**
 * Pre-26.1 only where the fullscreen Compose GUI draws onto the back buffer instead of the main
 * render target that screenshots read
 * <p>
 * Blit the back buffer into it before the screenshot reads
 */
@Mixin(Screenshot.class)
public class Mixin_ScreenshotComposite {

    //? if >= 1.21.5 {
    @Inject(method = "takeScreenshot(Lcom/mojang/blaze3d/pipeline/RenderTarget;Ljava/util/function/Consumer;)V", at = @At("HEAD"))
    private static void oneconfig$composite(RenderTarget target, Consumer<NativeImage> consumer, CallbackInfo ci) {
        SkiaCtx.INSTANCE.compositeBackBufferForScreenshot(target);
    }
    //? } else {
    /*@Inject(method = "takeScreenshot(Lcom/mojang/blaze3d/pipeline/RenderTarget;)Lcom/mojang/blaze3d/platform/NativeImage;", at = @At("HEAD"))
    private static void oneconfig$composite(RenderTarget target, CallbackInfoReturnable<NativeImage> cir) {
        SkiaCtx.INSTANCE.compositeBackBufferForScreenshot(target);
    }
    *///? }
}
