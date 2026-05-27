package org.polyfrost.oneconfig.internal.mixin.skia;

//? cinnabar {
/*import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "graphics.cinnabar.core.hg3d.Hg3DCommandEncoder", remap = false)
public class Mixin_CinnabarSkiaFlush {

    @Shadow
    void endCommandBuffers() {}

    @Shadow
    public void flush() {}

    @Inject(method = "presentTexture", at = @At("HEAD"), remap = false)
    private void impl$flushSkiaBeforePresent(CallbackInfo ci) {
        endCommandBuffers();
        flush();

        if (SkiaCtx.INSTANCE.isReady() && SkiaCtx.INSTANCE.isVulkanMode()) {
            SkiaCtx.INSTANCE.draw();
        }
    }
}
*///? }