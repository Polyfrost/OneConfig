package org.polyfrost.oneconfig.internal.mixin.skia;

import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.vulkanmod.vulkan.Renderer", remap = false)
public class Mixin_VulkanModSkiaFlush {

    @Inject(method = "endFrame", at = @At("HEAD"), remap = false, require = 0)
    private void impl$flushSkiaBeforePresent(CallbackInfo ci) {
        if (SkiaCtx.INSTANCE.isReady() && SkiaCtx.INSTANCE.isVulkanMode()) {
            SkiaCtx.INSTANCE.draw();
        }
    }
}
