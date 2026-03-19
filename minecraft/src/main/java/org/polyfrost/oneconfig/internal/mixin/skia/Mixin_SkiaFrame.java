package org.polyfrost.oneconfig.internal.mixin.skia;

import com.mojang.blaze3d.platform.Window;
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class Mixin_SkiaFrame {
    @Shadow
    private int framebufferWidth;

    @Shadow
    private int framebufferHeight;

    @Inject(method = "refreshFramebufferSize", at = @At("TAIL"))
    void impl$onResize(CallbackInfo ci) {
        SkiaCtx.INSTANCE.recreateSurface(this.framebufferWidth, this.framebufferHeight);
    }

    @Inject(method = "updateDisplay", at = @At("HEAD"), require = 0)
    void impl$onDraw(CallbackInfo ci) {
        if (!SkiaCtx.INSTANCE.isVulkanMode()) {
            SkiaCtx.INSTANCE.draw();
        }
    }

    @Inject(method = "updateDisplay(Lcom/mojang/blaze3d/TracyFrameCapture;)V", at = @At("HEAD"), require = 0)
    void impl$onDrawNew(CallbackInfo ci) {
        if (!SkiaCtx.INSTANCE.isVulkanMode()) {
            SkiaCtx.INSTANCE.draw();
        }
    }
}
