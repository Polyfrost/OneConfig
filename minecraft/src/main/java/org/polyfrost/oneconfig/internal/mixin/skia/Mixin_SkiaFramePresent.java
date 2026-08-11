package org.polyfrost.oneconfig.internal.mixin.skia;

//? >= 26.1 {
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx;
import net.minecraft.client.Minecraft;
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
            target = "Lcom/mojang/blaze3d/systems/GpuSurface;blitFromTexture(Lcom/mojang/blaze3d/systems/CommandEncoder;Lcom/mojang/blaze3d/textures/GpuTextureView;)V"))
    private void impl$onPresent(boolean tick, CallbackInfo ci) {
        SkiaCtx.INSTANCE.draw();
    }
}
//? } else {

/*// 26.1 removed Window.updateDisplay, which is where Mixin_SkiaFrame used to flush the GL Skia surface.
// Flush into the main render target before RenderTarget.blitToScreen (in renderFrame) so queued Compose/Skia
// draws are included by screenshots and by the window blit.
@Mixin(Minecraft.class)
public class Mixin_SkiaFramePresent {

    @Inject(method = "renderFrame", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen()V"))
    private void impl$onPresent(boolean tick, CallbackInfo ci) {
        SkiaCtx.INSTANCE.draw();
    }
}
*///? }
//? }
