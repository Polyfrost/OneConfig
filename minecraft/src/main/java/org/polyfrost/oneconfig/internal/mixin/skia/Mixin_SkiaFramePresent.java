package org.polyfrost.oneconfig.internal.mixin.skia;

//? > 1.8.9 {
import net.minecraft.client.Minecraft;
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 26.2 {
// 26.2 removed RenderSystem.flipFrame and presents inside Minecraft.renderFrame via
// GpuSurface.blitFromTexture so flush queued Compose/Skia draws before that blit or screenshots miss them
@Mixin(Minecraft.class)
public class Mixin_SkiaFramePresent {

    @Inject(method = "renderFrame", at = @At(value = "INVOKE",
            target = "Lcom/mojang/renderpearl/api/device/GpuSurface;blitFromTexture(Lcom/mojang/renderpearl/api/commands/CommandEncoder;Lcom/mojang/renderpearl/api/textures/GpuTextureView;)V"))
    private void impl$onPresent(boolean tick, CallbackInfo ci) {
        SkiaCtx.INSTANCE.draw();
    }
}
//? } elif >= 26.1 {

/*// Flush into the main render target before RenderTarget.blitToScreen (in renderFrame) so queued Compose/Skia
// draws are included by screenshots and by the window blit.
@Mixin(Minecraft.class)
public class Mixin_SkiaFramePresent {

    @Inject(method = "renderFrame", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen()V"))
    private void impl$onPresent(boolean tick, CallbackInfo ci) {
        SkiaCtx.INSTANCE.draw();
    }
}
*///? } else {

/*// Flush into the main render target before RenderTarget.blitToScreen so queued Compose/Skia draws are included
// by screenshots and by the window blit, including when another mod replaces that blit with its own presentation.
@Mixin(Minecraft.class)
public class Mixin_SkiaFramePresent {

    //~ if < 1.21.5 'blitToScreen()V' -> 'blitToScreen(II)V'
    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen()V"))
    private void impl$onPresent(boolean tick, CallbackInfo ci) {
        // VulkanMod flushes from Mixin_VulkanModSkiaFlush instead
        if (!SkiaCtx.INSTANCE.isVulkanMode()) {
            SkiaCtx.INSTANCE.draw();
        }
    }
}
*///? }
//? }
